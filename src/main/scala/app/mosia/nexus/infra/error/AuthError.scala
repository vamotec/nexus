package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object AuthError:
  case object InvalidCredentials extends ClientError:
    override def message: String = "Invalid email or password"

    override def code: String = "AUTH_INVALID_CREDENTIALS"

    override def httpStatus: StatusCode = StatusCode.Unauthorized

  case class InvalidToken(reason: String) extends ClientError:
    override def message: String = s"Invalid token: $reason"

    override def code: String = "AUTH_INVALID_TOKEN"

    override def httpStatus: StatusCode = StatusCode.Unauthorized

  case class InvalidChallenge(reason: String) extends ClientError:
    override def message: String = s"Invalid challenge: $reason"

    override def code: String = "AUTH_INVALID_CHALLENGE"

    override def httpStatus: StatusCode = StatusCode.Unauthorized

  case object TokenExpired extends ClientError:
    override def message: String = "Token has expired"

    override def code: String = "AUTH_TOKEN_EXPIRED"

    override def httpStatus: StatusCode = StatusCode.Unauthorized

  case class InsufficientPermissions(required: Set[String]) extends ClientError:
    override def message: String = s"Insufficient permissions. Required: ${required.mkString(", ")}"

    override def code: String = "AUTH_INSUFFICIENT_PERMISSIONS"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case class OauthParseFailed(reason: String) extends ClientError:
    override def message: String = s"Invalid oauth info: $reason"

    override def code: String = "OAUTH_PARSE_FAILED"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case object InvalidOauthEmail extends ClientError:
    override def message: String = "Invalid oauth email"

    override def code: String = "OAUTH_INVALID_EMAIL"

    override def httpStatus: StatusCode = StatusCode.Unauthorized

  case class UnsupportedProvider(provider: String) extends ClientError:
    override def message: String = s"Unsupported provider: $provider"

    override def code: String = "OAUTH_PROVIDER_UNSUPPORTED"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case class OauthStateExpired(id: String) extends ClientError:
    override def message: String = s"Oauth state expired: $id"

    override def code: String = "OAUTH_STATE_EXPIRED"

    override def httpStatus: StatusCode = StatusCode.Forbidden
