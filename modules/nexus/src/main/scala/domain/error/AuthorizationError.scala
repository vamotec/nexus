package app.mosia.nexus
package domain.error

/** ========== 403 Forbidden - 授权错误 ========== */
sealed trait AuthorizationError extends ClientError:
  def httpStatus: Int = 403

object AuthorizationError:
  case class PermissionDenied(
    action: String,
    resource: String,
    userId: Option[String] = None,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthorizationError:
    val message: String = userId match
      case Some(uid) => s"User $uid does not have permission to $action on $resource"
      case None => s"Permission denied: cannot $action on $resource"
    val errorCode = "PERMISSION_DENIED"

  case class InsufficientPermissions(
    requiredPermissions: Set[String],
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends AuthorizationError:
    val message   = s"Insufficient permissions. Required: ${requiredPermissions.mkString(", ")}"
    val errorCode = "INSUFFICIENT_PERMISSIONS"
