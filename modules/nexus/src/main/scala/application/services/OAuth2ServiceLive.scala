package app.mosia.nexus
package application.services

import application.dto.response.auth.{CallbackResponse, OAuthTokenResponse}
import application.dto.response.user.UserResponse
import application.states.OAuth2StateData
import domain.config.AppConfig
import domain.config.auth.OAuth2ClientConfig
import domain.error.*
import domain.model.user.{GitHubUserInfo, User}
import domain.services.app.{OAuth2Service, UserService}
import domain.services.infra.{JwtService, RedisService}

import zio.json.*
import zio.*
import zio.http.*

import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

final class OAuth2ServiceLive(
  config: AppConfig,
  client: Client,
  userService: UserService,
  jwtService: JwtService,
  redisService: RedisService
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
      .fromOption(config.auth.oauth.clients.get(provider))
      .orElseFail(InvalidInput("config", s"OAuth2 client config for provider $provider not found"))

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
        .mapError(err => InvalidInput("oauth url", s"Invalid authorization URL: $err"))
    yield url).mapError(toAppError)

  override def handleCallback(provider: String, code: String, stateId: String): ZIO[Scope, AppError, CallbackResponse] =
    (for
      clientConfig <- getClientConfig(provider)
      decodedUrl <- ZIO
        .fromEither(URL.decode(clientConfig.tokenUrl))
        .mapError(err => InvalidInput("oauth token url", s"Invalid token URL: $err"))
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
        .mapError(err => InvalidInput("oauth token", s"Failed to parse token response: $err"))
      _ <- ZIO
        .fail(InvalidInput("oauth token", s"Invalid token type: ${parsed.tokenType}"))
        .unless(parsed.tokenType.equalsIgnoreCase("bearer"))
      stateOpt <- redisService
        .get[OAuth2StateData](key = stateId)
      state <- ZIO
        .fromOption(stateOpt)
        .mapError(_ => InvalidInput("state", "from option"))
      userInfo <- getUserInfo(provider, parsed.accessToken)
      user <- oauthLogin(provider, userInfo)
      response = CallbackResponse(UserResponse.fromDomain(user), state.platform)
    yield response).mapError(toAppError)

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
    yield userInfo).mapError(toAppError)

  override def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String] =
    val url = queryParam
      .getOrElse("/")
    isValidReturnUrl(url)
      .filterOrFail(identity)(InvalidInput("url", s"Invalid return URL: $url"))
      .as(url)

  override def oauthLogin(provider: String, userInfo: String): AppTask[User] =
    provider match
      case "Github" =>
        for
          githubUserInfo <- ZIO
            .fromEither(userInfo.fromJson[GitHubUserInfo])
            .mapError(_ => InvalidInput(field = "info", reason = "parse info json failed"))
          email <- ZIO
            .fromOption(githubUserInfo.email)
            .orElseFail(InvalidInput("email", "from option"))
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
        ZIO.fail(NotFound("provider", provider))

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
