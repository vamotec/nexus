package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.application.dto.request.auth.OAuthLoginRequest
import app.mosia.nexus.application.service.auth.{AuthService, OAuth2Service}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.error.ErrorMapper
import app.mosia.nexus.presentation.http.endpoint.SecureEndpoints.baseEndpoint
import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class OAuth2Endpoint(oauth2Service: OAuth2Service, authService: AuthService) extends EndpointModule:
  override def endpoints: List[ZServerEndpoint[JwtService, ZioStreams]] =
    List(login, callback)

  val login: ZServerEndpoint[JwtService, ZioStreams] =
    baseEndpoint.get
      .in("api" / "oauth" / "login")
      .in(jsonBody[OAuthLoginRequest])
      .out(statusCode(StatusCode.Found))
      .out(header[String]("Location"))
      .description("request OAuth2 login")
      .zServerLogic { request =>
        (for {
          returnUrl <- oauth2Service.extractAndValidateReturnUrl(Some(request.returnUrl))
          platform = request.deviceInfo.map(_.platform)
          authUrl <- oauth2Service.getAuthorizationUrl(request.provider, returnUrl, platform)
        } yield authUrl.toString).mapError(ErrorMapper.toErrorResponse)
      }

  private val callback: ZServerEndpoint[JwtService, ZioStreams] =
    baseEndpoint.get
      .in("api" / "oauth" / "callback" / path[String]("provider"))
      .in(query[String]("code"))
      .in(query[String]("state"))
      .out(statusCode(StatusCode.Found))
      .out(header[String]("Location"))
      .out(setCookies)
      .zServerLogic { case (provider, code, state) =>
        ZIO
          .scoped {
            for
              result <- oauth2Service.handleCallback(provider, code, state)
              userId <- UserId.fromStringZIO(result.user.id)
              accessToken <- authService.generateAccessToken(userId, result.platform)
              refreshToken <- authService.generateRefreshToken(userId, result.platform)

              /** TODO: 前端需要做一个中间页来进行跳转 */
              redirectUrl <- oauth2Service.buildRedirectUrl(accessToken, refreshToken, result.platform)
              cookies <- authService.buildAuthCookies(accessToken, refreshToken)
            yield (redirectUrl, cookies)
          }
          .mapError(ErrorMapper.toErrorResponse)
      }
