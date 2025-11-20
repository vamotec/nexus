package app.mosia.nexus
package presentation.http.endpoint

import application.dto.request.auth.{LoginRequest, RefreshTokenRequest}
import application.dto.response.auth.{LoginResponse, RefreshTokenResponse}
import application.dto.response.common.ApiResponse
import domain.error.*
import domain.services.app.{AuditService, AuthService, DeviceService}
import domain.services.infra.JwtService
import presentation.http.endpoint.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.{Endpoint, EndpointOutput}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class AuthEndpoint(
  authService: AuthService,
  deviceService: DeviceService,
  auditService: AuditService
) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    List(login, refresh)

  override def secureEndpoints: List[ZServerEndpoint[JwtService, ZioStreams]] = List.empty

  // 统一的登录端点
  private val login: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("api" / "auth" / "login")
      .in(jsonBody[LoginRequest])
      .out(jsonBody[ApiResponse[LoginResponse]])
      .out(setCookies)
      .zServerLogic { request =>
        (for
          /** 1. 认证用户 */
          user <- request match
            case LoginRequest(Some(email), Some(password), None, _) =>
              authService.passwordLogin(email, password)
            case LoginRequest(None, None, Some(bioRequest), _) =>
              authService.biometricLogin(bioRequest)
            case _ =>
              ZIO.fail(InvalidCredentials())

          /** 2. 根据平台生成 token（不同过期时间） */
          platform = request.deviceInfo.map(_.platform)
          accessToken <- authService.generateAccessToken(user.id, platform)
          refreshToken <- authService.generateRefreshToken(user.id, platform)

          /** 3. 注册设备（仅移动端） */
          _ <- request.deviceInfo match
            case Some(deviceInfo) =>
              deviceService.registerOrUpdateDevice(user.id, deviceInfo)
            case None =>
              ZIO.unit

          /** 4. 记录登录日志 */
          _ <- auditService.logLogin(user.id, platform)

          /** 5. 构建响应 */
          response <- authService.buildLoginResponse(accessToken, refreshToken, user)
          cookies <- authService.buildAuthCookies(accessToken, refreshToken)
        yield (ApiResponse(response), cookies)).mapError(toErrorResponse)
      }

  private val refresh: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("api" / "auth" / "refresh") // 或 "token" / "refresh"
      .in(jsonBody[RefreshTokenRequest]) // 从请求体读取
      .out(jsonBody[ApiResponse[RefreshTokenResponse]]) // JSON 响应
      .out(setCookies)
      .zServerLogic { request =>
        (for {
          userId <- authService.validateRefreshToken(request.refreshToken)
          platform = request.deviceInfo.map(_.platform)
          newAccessToken <- authService.generateAccessToken(userId, platform)
          newRefreshToken <- authService.generateRefreshToken(userId, platform)
          _ <- authService.rotateRefreshToken(
            userId = userId,
            oldToken = request.refreshToken,
            newToken = newRefreshToken
          )
          response <- authService.buildRefreshTokenResponse(newAccessToken, newRefreshToken)
          cookies <- authService.buildAuthCookies(newAccessToken, newRefreshToken)
        } yield (ApiResponse(data = response), cookies))
          .mapError(toErrorResponse)
      }
