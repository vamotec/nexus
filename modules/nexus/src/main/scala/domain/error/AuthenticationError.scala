package app.mosia.nexus
package domain.error

/** ========== 401 Unauthorized - 认证错误 ========== */
sealed trait AuthenticationError extends ClientError:
  def httpStatus: Int = 401

object AuthenticationError:
  case class Unauthenticated(
    reason: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthenticationError:
    val message   = s"Authentication required: $reason"
    val errorCode = "UNAUTHENTICATED"

  case class InvalidCredentials(
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthenticationError:
    val message   = "Invalid credentials"
    val errorCode = "INVALID_CREDENTIALS"

  case class InvalidChallenge(
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthenticationError:
    val message   = "Invalid or expired challenge"
    val errorCode = "INVALID_CHALLENGE"

  case class InvalidDevice(
    filed: String,
    value: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthenticationError:
    val message   = "Invalid device: "
    val errorCode = "INVALID_CHALLENGE"

  case class TokenExpired(
    tokenType: String = "access_token",
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthenticationError:
    val message   = s"$tokenType has expired"
    val errorCode = "TOKEN_EXPIRED"
