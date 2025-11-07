package app.mosia.nexus.infra.error

import zio.ZIO

object ZIOErrorHandling:
  // 自动错误转换
  def autoMapError[R, A](effect: ZIO[R, Throwable, A]): ZIO[R, AppError, A] =
    effect.mapError(ErrorMapper.toAppError)

  // 验证辅助方法
  def validateEmail(email: String): AppTask[String] =
    if (email.contains("@")) ZIO.succeed(email)
    else ZIO.fail(ValidationError.InvalidEmail(email))

  def validatePassword(password: String): AppTask[String] =
    if (password.length >= 8) ZIO.succeed(password)
    else ZIO.fail(ValidationError.InvalidPassword("Password must be at least 8 characters"))

  // 资源存在性检查
  def requireExists[R, A](effect: ZIO[R, Throwable, Option[A]], ifNone: => AppError): ZIO[R, AppError, A] =
    autoMapError(effect).flatMap {
      case Some(value) => ZIO.succeed(value)
      case None => ZIO.fail(ifNone)
    }

  // 权限检查
  def requirePermission(hasPermission: Boolean, required: Set[String]): AppTask[Unit] =
    if (hasPermission) ZIO.unit
    else ZIO.fail(AuthError.InsufficientPermissions(required))
