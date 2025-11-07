package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object ExternalServiceError:
  case class IsaacServiceError(reason: String) extends ExternalServiceError:
    override def message: String = s"Isaac Sim service error: $reason"

    override def code: String = "EXTERNAL_ISAAC_ERROR"

    override def httpStatus: StatusCode = StatusCode.BadGateway

  case class DatabaseError(cause: Throwable) extends ExternalServiceError:
    override def message: String = "Database operation failed"

    override def code: String = "EXTERNAL_DATABASE_ERROR"

    override def httpStatus: StatusCode = StatusCode.InternalServerError

    override def details: Option[String] = Some(cause.getMessage)
