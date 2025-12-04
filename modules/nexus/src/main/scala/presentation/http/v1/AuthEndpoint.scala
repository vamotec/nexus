package app.mosia.nexus
package presentation.http.v1

import application.dto.request.auth.*
import application.dto.response.auth.{LoginResponse, RefreshTokenResponse, RegisterResponse, VerifyEmailResponse}
import application.dto.response.common.ApiResponse
import domain.error.*
import domain.model.verification.VerificationCodeType
import domain.services.app.*
import domain.services.infra.JwtContent
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.baseEndpoint

import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class AuthEndpoint(
  authService: AuthService,
  deviceService: DeviceService,
  auditService: AuditService,
  notificationService: NotificationService,
  userService: UserService
) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    List(login, register, verifyEmail, resendVerificationCode, forgotPassword, resetPassword, refresh)

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] = List.empty

  // 统一的登录端点
  private val login: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "login")
      .in(jsonBody[AuthRequest])
      .out(jsonBody[ApiResponse[LoginResponse]])
      .out(setCookies)
      .zServerLogic { request =>
        (for
          /** 1. 认证用户 */
          user <- request match
            case AuthRequest(Some(email), Some(password), None, None, _) =>
              authService.passwordLogin(email, password)
            case AuthRequest(None, None, None, Some(bioRequest), _) =>
              authService.biometricLogin(bioRequest)
            case _ =>
              ZIO.fail(InvalidCredentials("login"))

          /** 2. 根据平台生成 token（不同过期时间） */
          platform = request.deviceInfo.map(_.platform)
          accessToken <- authService.generateAccessToken(user.id.value.toString, platform)
          refreshToken <- authService.generateRefreshToken(user.id.value.toString, platform)

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

  private val register: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "register")
      .in(jsonBody[AuthRequest])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[ApiResponse[RegisterResponse]])
      .zServerLogic { request =>
        (for
          user <- request match
            case AuthRequest(Some(email), Some(password), Some(name), None, _) =>
              authService.registerUser(email, password, name)
            case _ =>
              ZIO.fail(InvalidCredentials("register"))
          verificationToken <- notificationService.sendEmailVerificationCode(user.email, VerificationCodeType.Email)
          // 生成临时验证令牌用于前端会话验证
          response = RegisterResponse(
            userId = user.id.value.toString,
            email = user.email,
            emailVerified = false,
            message = s"注册成功，验证邮件已发送至 ${user.email}",
            verificationToken = Some(verificationToken)
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val verifyEmail: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "verify-email")
      .in(jsonBody[VerifyEmailRequest])
      .out(jsonBody[ApiResponse[VerifyEmailResponse]])
      .zServerLogic { request =>
        (for
          // 1. 验证验证码
          isValid <- notificationService.verifyEmailCode(
            email = request.email,
            code = request.code,
            token= request.token,
            codeType = VerificationCodeType.Email
          )
          // 2. 如果验证失败，返回错误
          _ <- if !isValid then
            ZIO.fail(InvalidInput("code", "验证码无效或已过期"))
          else
            ZIO.unit

          // 3. 更新用户的邮箱验证状态
          _ <- userService.markEmailAsVerified(request.email)

          // 4. 构建响应
          response = VerifyEmailResponse(
            success = true,
            message = "邮箱验证成功"
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val resendVerificationCode: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "resend-verification-code")
      .in(jsonBody[ResendVerificationCodeRequest])
      .out(jsonBody[ApiResponse[RegisterResponse]])
      .zServerLogic { request =>
        (for
          // 1. 重新发送验证码（包含限流检查）
          _ <- notificationService.sendEmailVerificationCode(
            email = request.email,
            codeType = request.codeType
          )

          // 2. 构建响应
          response = RegisterResponse(
            userId = "", // 重新发送不需要 userId
            email = request.email,
            emailVerified = false,
            message = s"验证码已重新发送至 ${request.email}"
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val forgotPassword: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "forgot-password")
      .in(jsonBody[ForgotPasswordRequest])
      .out(jsonBody[ApiResponse[RegisterResponse]])
      .zServerLogic { request =>
        (for
          // 1. 检查用户是否存在
          userOpt <- userService.findByEmail(request.email)
          _ <- userOpt match
            case None => ZIO.fail(NotFound("User", request.email))
            case Some(_) => ZIO.unit

          // 2. 发送重置密码验证码
          _ <- notificationService.sendEmailVerificationCode(
            email = request.email,
            codeType = VerificationCodeType.ResetPassword
          )

          // 3. 构建响应
          response = RegisterResponse(
            userId = "",
            email = request.email,
            emailVerified = false,
            message = s"重置密码验证码已发送至 ${request.email}"
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val resetPassword: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "reset-password")
      .in(jsonBody[ResetPasswordRequest])
      .out(jsonBody[ApiResponse[VerifyEmailResponse]])
      .zServerLogic { request =>
        (for
          // 1. 验证验证码
          isValid <- notificationService.verifyEmailCode(
            email = request.email,
            code = request.code,
            token = request.token,
            codeType = VerificationCodeType.ResetPassword
          )

          // 2. 如果验证失败，返回错误
          _ <- if !isValid then
            ZIO.fail(InvalidInput("code", "验证码无效或已过期"))
          else
            ZIO.unit

          // 3. 重置密码
          _ <- userService.resetPassword(request.email, request.newPassword)

          // 4. 构建响应
          response = VerifyEmailResponse(
            success = true,
            message = "密码重置成功，请使用新密码登录"
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }

  private val refresh: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("auth" / "refresh") // 或 "token" / "refresh"
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
