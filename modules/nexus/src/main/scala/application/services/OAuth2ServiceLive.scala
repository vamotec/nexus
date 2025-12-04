package app.mosia.nexus
package application.services

import application.dto.response.auth.{CallbackResponse, ProviderTokenResponse}
import application.dto.response.user.UserResponse
import application.states.OAuth2StateData
import domain.config.AppConfig
import domain.config.auth.OAuth2ClientConfig
import domain.error.*
import domain.model.user.*
import domain.model.user.Provider.{GitHub, toStr}
import domain.repository.OAuthProviderRepository
import domain.services.app.{OAuth2Service, UserService}
import domain.services.infra.{JwtService, RedisService}

import zio.*
import zio.http.*
import zio.json.*

import java.net.{URI, URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

final class OAuth2ServiceLive(
  config: AppConfig,
  client: Client,
  repo: OAuthProviderRepository,
  userService: UserService,
  jwtService: JwtService,
  redis: RedisService
) extends OAuth2Service:
  private def callbackUri(provider: Provider): String =
    val str = toStr(provider)
    s"http://${config.auth.baseUrl}/api/oauth/callback/$str"
  
  private def getClientConfig(provider: Provider): AppTask[OAuth2ClientConfig] =
    ZIO
      .fromOption(config.auth.oauth.clients.get(toStr(provider)))
      .orElseFail(InvalidInput("config", s"OAuth2 client config for provider $provider not found"))

  override def getAuthorizationUrl(providerStr: String, returnUrl: String, platform: Option[String]): AppTask[(URL, String)] =
    (for
      stateId <- ZIO.attempt(UUID.randomUUID().toString)
      provider = Provider.fromString(providerStr)
      _ <- redis.set(
        key = stateId,
        value = OAuth2StateData(
          provider = provider,
          redirectUri = returnUrl,
          platform = platform,
          createdAt = Instant.now()
        ).toJson,
        ttlSeconds = 5.minutes.getSeconds
      )
      clientConfig <- getClientConfig(provider)
      params = Map(
        "client_id" -> clientConfig.clientId,
        "scope" -> clientConfig.scope,
        "response_type" -> "code",
        "state" -> stateId
      )
      query = params
        .map { case (k, v) => s"$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}" }
        .mkString("&")
      url <- ZIO
        .fromEither(URL.decode(s"${clientConfig.authUrl}?$query"))
        .mapError(err => InvalidInput("oauth url", s"Invalid authorization URL: $err"))
    yield (url, stateId)).mapError(toAppError)

  override def handleCallback(providerStr: String, code: String, stateId: String): AppTask[CallbackResponse] =
    (for
      stateOpt <- redis.get(key = stateId)
      state <- ZIO
        .fromOption(stateOpt)
        .mapError(_ => InvalidInput("state", "from option"))
        .flatMap(str =>
          ZIO.fromEither(str.fromJson[OAuth2StateData])
            .mapError(err => InvalidInput("state", s"invalid JSON: $err"))
        )
      provider <- ZIO.succeed(Provider.fromString(providerStr))
      oautTokenResponse <- exchangeToken(provider, code)
      _ <- ZIO
        .fail(InvalidInput("oauth token", s"Invalid token type: ${oautTokenResponse.tokenType}"))
        .unless(oautTokenResponse.tokenType.equalsIgnoreCase("bearer"))
      userInfo <- getUserInfo(provider, oautTokenResponse.accessToken)
      user <- oauthLogin(provider, userInfo)
      response = CallbackResponse(UserResponse.fromDomain(user), state.redirectUri, state.platform)
    yield response).mapError(toAppError)

  override def getUserInfo(provider: Provider, accessToken: String): AppTask[String] =
    ZIO.scoped:
      (for
        clientConfig <- getClientConfig(provider)
        userInfo <- client
          .request(
            Request
              .get(URL.decode(clientConfig.userUrl).toOption.get)
              .addHeaders(Headers(Header.Authorization.Bearer(accessToken)))
          )
          .flatMap(_.body.asString)
      yield userInfo).mapError(toAppError)

  override def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String] =
    val url = queryParam
      .map(encoded => URLDecoder.decode(encoded, StandardCharsets.UTF_8)) // 添加解码
      .getOrElse("/")
    isValidReturnUrl(url)
      .filterOrFail(identity)(InvalidInput("url", s"Invalid return URL: $url"))
      .as(url)

  override def oauthLogin(provider: Provider, userInfo: String): AppTask[User] =
    provider match
      case GitHub =>
        for
          githubUserInfo <- ZIO
            .fromEither(userInfo.fromJson[GitHubUserInfo])
            .mapError(_ => InvalidInput("info", "parse info json failed"))

          // 1. 先通过OAuth绑定查找
          existingOAuthOpt <- repo.findByProviderAndProviderId(
            GitHub,
            githubUserInfo.id.toString
          )

          user <- existingOAuthOpt match
            case Some(oauthProvider) =>
              // 已绑定,直接获取用户并更新最后使用时间
              for
                user <- userService.findById(oauthProvider.userId.value.toString)
                  .someOrFail(NotFound("user", oauthProvider.userId.toString))
                _ <- repo.updateLastUsed(GitHub, githubUserInfo.id.toString, Instant.now())
              yield user

            case None =>
              // 2. 未绑定,检查邮箱是否存在用户
              val email = githubUserInfo.email.getOrElse(
                s"${githubUserInfo.login}@github.oauth"
              )

              userService.findByEmail(email).flatMap {
                case Some(existingUser) =>
                  // 用户存在,绑定OAuth
                  userService.linkProvider(
                    userId = existingUser.id,
                    provider = GitHub,
                    providerUserId = githubUserInfo.id.toString,
                    providerEmail = githubUserInfo.email
                  ).as(existingUser)

                case None =>
                  // 3. 全新用户,在事务中创建用户+OAuth绑定
                  userService.createUserWithOAuth(
                    email = email,
                    username = Some(githubUserInfo.login),
                    provider = GitHub,
                    providerUserId = githubUserInfo.id.toString,
                    providerEmail = githubUserInfo.email
                  )
              }
        yield user

      case _ =>
        ZIO.fail(NotFound("provider", provider.toString))

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

  private def exchangeToken(provider: Provider, code: String): AppTask[ProviderTokenResponse] =
    ZIO.scoped:
      (for
        clientConfig <- getClientConfig(provider)
        redirectUri <- ZIO.succeed(callbackUri(provider))
        body = Body.fromURLEncodedForm(
          Form(
            FormField.simpleField("code", code),
            FormField.simpleField("client_id", clientConfig.getClientId),
            FormField.simpleField("client_secret", clientConfig.getClientSecret),
            FormField.simpleField("redirect_uri", redirectUri)
          )
        )
        request = Request
          .post(clientConfig.tokenUrl, body)
          .addHeader(Header.Accept(MediaType.application.json))
        response <- client.request(request)
        json <- response.body.asString
        tokenResp <- ZIO.fromEither(
          json.fromJson[ProviderTokenResponse]
        ).mapError(err => InvalidInput("oauth token", s"Failed to parse token response: $err"))

        _ <- ZIO.logDebug(s"Token exchange: ${tokenResp.accessToken.take(20)}...")

      yield tokenResp).mapError(toAppError)

  private def createSecureCookie(
    name: String,
    value: String,
    maxAge: Duration
  ): String =
    val maxAgeSeconds = maxAge.toSeconds
    s"$name=$value; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=$maxAgeSeconds"

object OAuth2ServiceLive:
  val live: ZLayer[AppConfig & Client & OAuthProviderRepository & UserService & JwtService & RedisService, Nothing, OAuth2Service] =
    ZLayer.fromFunction(new OAuth2ServiceLive(_, _, _, _, _, _))
