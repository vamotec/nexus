package app.mosia.nexus
package domain.error

/** ==================== 服务端错误 (5xx) ==================== */
/** 服务端错误需要详细的堆栈跟踪日志 */
trait ServerError extends AppError:
  def shouldLogDetailed: Boolean = true
