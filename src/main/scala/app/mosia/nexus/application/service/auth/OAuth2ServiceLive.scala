package app.mosia.nexus.application.service.auth

import app.mosia.nexus.application.dto.response.auth.{CallbackResponse, OAuthResponse, OAuthTokenResponse}
import app.mosia.nexus.application.dto.response.user.UserResponse
import app.mosia.nexus.application.service.user.UserService
import app.mosia.nexus.domain.model.user.{GitHubUserInfo, User}
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.config.{AppConfig, OAuth2ClientConfig}
import app.mosia.nexus.infra.error.*
import app.mosia.nexus.infra.persistence.redis.RedisService
import app.mosia.nexus.infra.states.OAuth2StateData
import zio.{durationInt, Duration, Scope, Task, ZIO, ZLayer}
import zio.http.*
import zio.json.*

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

final class OAuth2ServiceLive(
  config: AppConfig,
  client: Client,
  userService: UserService,
  jwtService: JwtService,
  redisService: RedisService,
) extends OAuth2Service:
  private def callbackUri(provider: String): String =
    s"${config.auth.baseUrl}/api/oauth/callback/$provider"

  override def buildRedirectUrl(accessToken: String, refreshToken: String, platform: Option[String]): AppTask[String] =
    ZIO.succeed:
      val params = Map(
        "accessToken" -> accessToken,
        "refreshToken" -> refreshToken,
        "platform" -> platform.getOrElse("web")
      ).map { case (k, v) =>
        s"$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
      }.mkString("&")
      s"${config.auth.baseUrl}/oauth/success?$params" // ✅ 完整 URL

  private def getClientConfig(provider: String): AppTask[OAuth2ClientConfig] =
    ZIO
      .fromOption(config.auth.oauth2.clients.get(provider))
      .orElseFail(OauthParseFailed(s"OAuth2 client config for provider $provider not found"))

  override def getAuthorizationUrl(provider: String, returnUrl: String, platform: Option[String]): AppTask[URL] =
    (for
      stateId <- ZIO.attempt(UUID.randomUUID().toString)
      _ <- redisService.set(
        key = stateId,
        value = OAuth2StateData(
          provider = provider,
          platform = platform,
          createdAt = Instant.now()
        ),
        expiration = Some(5.minutes)
      )
      clientConfig <- getClientConfig(provider)
      redirectUri <- ZIO.succeed(callbackUri(provider))
      params = Map(
        "client_id" -> clientConfig.clientId,
        "redirect_uri" -> redirectUri,
        "scope" -> clientConfig.scope,
        "response_type" -> "code",
        "state" -> stateId
      )
      query = params
        .map { case (k, v) => s"$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}" }
        .mkString("&")
      url <- ZIO
        .fromEither(URL.decode(s"${clientConfig.authorizationUrl}?$query"))
        .mapError(err => OauthParseFailed(s"Invalid authorization URL: $err"))
    yield url).mapError(ErrorMapper.toAppError)

  override def handleCallback(provider: String, code: String, stateId: String): ZIO[Scope, AppError, CallbackResponse] =
    (for
      clientConfig <- getClientConfig(provider)
      decodedUrl <- ZIO
        .fromEither(URL.decode(clientConfig.tokenUrl))
        .mapError(err => OauthParseFailed(s"Invalid token URL: $err"))
      redirectUri <- ZIO.succeed(callbackUri(provider))
      requestBody = Map(
        "client_id" -> clientConfig.clientId,
        "client_secret" -> clientConfig.clientSecret,
        "code" -> code,
        "redirect_uri" -> redirectUri
      ).toJson
      oautTokenResponse <- client
        .request(
          Request
            .post(decodedUrl, Body.fromString(requestBody))
            .addHeaders(
              Headers(
                Header.ContentType(MediaType.application.json),
                Header.Accept(MediaType.application.json) // 🔑 关键：请求 JSON 响应
              )
            )
        )
        .flatMap(_.body.asString)
      parsed <- ZIO
        .fromEither(oautTokenResponse.fromJson[OAuthTokenResponse])
        .mapError(err => OauthParseFailed(s"Failed to parse token response: $err"))
      _ <- ZIO
        .fail(OauthParseFailed(s"Invalid token type: ${parsed.tokenType}"))
        .unless(parsed.tokenType.equalsIgnoreCase("bearer"))
      state <- redisService
        .get[OAuth2StateData](key = stateId)
        .someOrFail(OauthStateExpired(stateId))
      userInfo <- getUserInfo(provider, parsed.accessToken)
      user <- oauthLogin(provider, userInfo)
      response = CallbackResponse(UserResponse.fromDomain(user), state.platform)
    yield response).mapError(ErrorMapper.toAppError)

  override def getUserInfo(provider: String, accessToken: String): ZIO[Scope, AppError, String] =
    (for
      clientConfig <- getClientConfig(provider)
      userInfo <- client
        .request(
          Request
            .get(URL.decode(clientConfig.userInfoUrl).toOption.get)
            .addHeaders(Headers(Header.Authorization.Bearer(accessToken)))
        )
        .flatMap(_.body.asString)
    yield userInfo).mapError(ErrorMapper.toAppError)

  override def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String] =
    val url = queryParam
      .getOrElse("/")
    isValidReturnUrl(url)
      .filterOrFail(identity)(OauthParseFailed(s"Invalid return URL: $url"))
      .as(url)

  override def oauthLogin(provider: String, userInfo: String): AppTask[User] =
    provider match
      case "Github" =>
        for
          githubUserInfo <- ZIO
            .fromEither(userInfo.fromJson[GitHubUserInfo])
            .mapError(e => OauthParseFailed(e))
          email <- ZIO
            .fromOption(githubUserInfo.email)
            .orElseFail(InvalidOauthEmail)
          userOpt <- userService.findByEmail(email)
          user <- userOpt match
            case Some(existingUser) =>
              // User exists, potentially update and return
              ZIO.succeed(existingUser)
            case None =>
              // User does not exist, create a new one
              val randomPassword = UUID.randomUUID().toString // OAuth users don't need a real password
              userService.createUser(email, randomPassword)
        yield user
      case _ =>
        ZIO.fail(UnsupportedProvider(provider))

  def isValidReturnUrl(url: String): AppTask[Boolean] =
    if (url == "/") ZIO.succeed(true)
    else
      val allowedOrigins = Set(
        "https://mosia.app",
        "https://www.mosia.app",
        "http://localhost:3000",
        "http://localhost:8080"
      )
      ZIO
        .attempt {
          val uri  = new URI(url)
          val port = uri.getPort match
            case -1 | 80 | 443 => ""
            case p => s":$p"
          val origin = s"${uri.getScheme}://${uri.getHost}$port"
          allowedOrigins.contains(origin)
        }
        .catchAll(_ => ZIO.succeed(false)) // URI 解析失败返回 false

  private def createSecureCookie(
    name: String,
    value: String,
    maxAge: Duration
  ): String =
    val maxAgeSeconds = maxAge.toSeconds
    s"$name=$value; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=$maxAgeSeconds"

object OAuth2ServiceLive:
  val live: ZLayer[AppConfig & Client & UserService & JwtService & RedisService, Throwable, OAuth2Service] =
    ZLayer.fromFunction(new OAuth2ServiceLive(_, _, _, _, _))
