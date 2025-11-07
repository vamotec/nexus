package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object DomainError:
  case object UserNotFound extends ClientError:
    override def message: String = "User not found"

    override def code: String = "DOMAIN_USER_NOT_FOUND"

    override def httpStatus: StatusCode = StatusCode.NotFound

  case object UsernameAlreadyExists extends ClientError:
    override def message: String = "Username already exists"

    override def code: String = "DOMAIN_USERNAME_EXISTS"

    override def httpStatus: StatusCode = StatusCode.Conflict

  case object ProjectNotFound extends ClientError:
    override def message: String = "Project not found"

    override def code: String = "DOMAIN_PROJECT_NOT_FOUND"

    override def httpStatus: StatusCode = StatusCode.NotFound

  case object AccessDenied extends ClientError:
    override def message: String = "Access denied to resource"

    override def code: String = "DOMAIN_ACCESS_DENIED"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case object AuditLogNotFound extends ClientError:
    override def message: String = "Auditlog not found"

    override def code: String = "AUDITLOG_NOT_FOUND"

    override def httpStatus: StatusCode = StatusCode.NotFound

  case class InvalidAuditData(msg: String) extends ClientError:
    override def message: String = s"Invalid audit data: $msg"

    override def code: String = "AUDIT_INVALID_DATA"

    override def httpStatus: StatusCode = StatusCode.Forbidden
