package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object InternalError:
  case class ConfigurationError(configs: String) extends ServerError:
    override def message: String = "Configuration error"

    override def code: String = "INTERNAL_CONFIG_ERROR"

    override def httpStatus: StatusCode = StatusCode.InternalServerError

    override def details: Option[String] = Some(configs)

  case class UnexpectedError(cause: Throwable) extends ServerError:
    override def message: String = "An unexpected error occurred"

    override def code: String = "INTERNAL_UNEXPECTED_ERROR"

    override def httpStatus: StatusCode = StatusCode.InternalServerError

    override def details: Option[String] = Some(cause.getMessage)

  final case class InvalidUserRole(str: String) extends AppError.ServerError:
    override def message: String         = s"Invalid user role value in database: $str"
    override def code: String            = "INVALID_USER_ROLE"
    override def httpStatus: StatusCode  = StatusCode.InternalServerError
    override def details: Option[String] = Some(str)
