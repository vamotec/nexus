package app.mosia.nexus
package domain.error

/** ========== 409 Conflict - 资源冲突 ========== */
sealed trait ConflictError extends ClientError:
  def httpStatus: Int = 409

object ConflictError:
  /** 重复实体 */
  case class DuplicateEntity(
    entityType: String,
    identifier: String,
    constraintName: Option[String] = None,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ConflictError:
    val message: String = constraintName match
      case Some(constraint) =>
        s"$entityType with identifier '$identifier' already exists (constraint: $constraint)"
      case None =>
        s"$entityType with identifier '$identifier' already exists"
    val errorCode = "DUPLICATE_ENTITY"

  /** 资源状态冲突 */
  case class StateConflict(
    entityType: String,
    currentState: String,
    attemptedAction: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ConflictError:
    val message =
      s"Cannot $attemptedAction $entityType in state '$currentState'"
    val errorCode = "STATE_CONFLICT"

  /** 并发修改冲突 */
  case class ConcurrentModification(
    entityType: String,
    identifier: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ConflictError:
    val message =
      s"$entityType '$identifier' was modified by another operation. Please retry."
    val errorCode = "CONCURRENT_MODIFICATION"

  case class AlreadyExists(
    filed: String,
    value: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ConflictError:
    val message   = s"$filed: $value already exists"
    val errorCode = "ALREADY_EXISTS"
