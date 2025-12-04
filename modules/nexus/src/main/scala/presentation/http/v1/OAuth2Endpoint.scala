package app.mosia.nexus
package presentation.http.v1

import application.dto.response.auth.OAuthUrlResponse
import application.dto.response.common.ApiResponse
import domain.error.AppError.toErrorResponse
import domain.services.app.{AuthService, OAuth2Service}
import domain.services.infra.JwtContent
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.model.headers.CookieWithMeta
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

final class OAuth2Endpoint(oauth2Service: OAuth2Service, authService: AuthService) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    List(login, callback)

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] = List.empty

  val login: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.get
      .in("oauth" / "login")
      .in(query[String]("provider"))
      .in(query[String]("returnUrl"))
      .in(query[Option[String]]("platform"))
      .out(jsonBody[ApiResponse[OAuthUrlResponse]]) // 改为返回 JSON
      .description("Get OAuth2 authorization URL")
      .zServerLogic { case (provider, returnUrl, platform) =>
        (for {
          returnUrl <- oauth2Service.extractAndValidateReturnUrl(Some(returnUrl))
          (authUrl, stateId) <- oauth2Service.getAuthorizationUrl(provider, returnUrl, platform)
          // 返回完整信息
          response = OAuthUrlResponse(
            authUrl = authUrl.toString,
            state = stateId
          )
        } yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val callback: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.get
      .in("oauth" / "callback" / path[String]("provider"))
      .in(query[String]("code"))
      .in(query[String]("state"))
      .out(statusCode(StatusCode.Found))
      .out(header[String]("Location"))
      .out(setCookies)
      .zServerLogic { case (provider, code, state) =>
        (for {
          result <- oauth2Service.handleCallback(provider, code, state)
          access <- authService.generateAccessToken(result.user.id, result.platform)
          refresh <- authService.generateRefreshToken(result.user.id, result.platform)
          cookies <- authService.buildAuthCookies(access, refresh)
        } yield {
          result.platform match {
            case Some("ios") | Some("android") =>
              // 移动端: Deep Link + 空 cookies
              val deepLink = s"mosia://oauth/callback?access_token=$access&refresh_token=$refresh"
              (deepLink, List.empty[CookieWithMeta])

            case _ =>
              // Web 端: 前端 URL + cookies
              (result.redirectUri, cookies)
          }
        }).mapError(toErrorResponse)
      }