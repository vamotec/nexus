package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object ValidationError:
  case class InvalidEmail(email: String) extends ClientError:
    override def message: String = s"Invalid email format: $email"

    override def code: String = "VALIDATION_INVALID_EMAIL"

    override def httpStatus: StatusCode = StatusCode.BadRequest

  case class InvalidPassword(reason: String) extends ClientError:
    override def message: String = s"Invalid password: $reason"

    override def code: String = "VALIDATION_INVALID_PASSWORD"

    override def httpStatus: StatusCode = StatusCode.BadRequest

  case class InvalidInput(field: String, reason: String) extends ClientError:
    override def message: String = s"Invalid $field: $reason"

    override def code: String = "VALIDATION_INVALID_INPUT"

    override def httpStatus: StatusCode = StatusCode.BadRequest
