package app.mosia.nexus
package domain.error

/** ==================== 服务端错误 (5xx) ==================== */
/** 服务端错误需要详细的堆栈跟踪日志 */
sealed trait InternalError extends ServerError:
  def httpStatus: Int = 500

object InternalError:
  /** 未预期的错误 */
  case class UnexpectedError(
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends InternalError:
    val message   = s"Unexpected error: ${cause.getClass.getSimpleName}: ${cause.fold("")(_.getMessage)}"
    val errorCode = "UNEXPECTED_ERROR"

  /** 配置错误 */
  case class ConfigurationError(
    setting: String,
    reason: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends InternalError:
    val message   = s"Configuration error for '$setting': $reason"
    val errorCode = "CONFIGURATION_ERROR"

  /** 无效的系统状态 */
  case class InvalidSystemState(
    component: String,
    currentState: String,
    expectedState: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends InternalError:
    val message =
      s"Invalid state in $component: expected $expectedState but was $currentState"
    val errorCode = "INVALID_SYSTEM_STATE"

  /** 资源耗尽 */
  case class ResourceExhausted(
    resource: String,
    details: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends InternalError:
    val message   = s"Resource exhausted: $resource - $details"
    val errorCode = "RESOURCE_EXHAUSTED"
