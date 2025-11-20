package app.mosia.nexus
package domain.error

/** ==================== 外部服务错误 (5xx) ==================== */
trait ExternalServiceError extends ServerError

object ExternalServiceError:
  /** ========== 502 Bad Gateway - gRPC 服务错误 ========== */
  case class GrpcServiceError(
    service: String,
    method: String,
    status: io.grpc.Status,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ExternalServiceError:
    val message   = s"gRPC error calling $service.$method: ${status.getCode} - ${status.getDescription}"
    val errorCode = "GRPC_SERVICE_ERROR"

    def httpStatus: Int = 502

  /** ========== 503 Service Unavailable - 计算中心不可用 ========== */
  case class ComputeCenterUnavailable(
    centerName: String,
    operation: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ExternalServiceError:
    val message   = s"Compute center '$centerName' unavailable during $operation: ${cause.fold("")(_.getMessage)}"
    val errorCode = "COMPUTE_CENTER_UNAVAILABLE"

    def httpStatus: Int = 503

  /** ========== 504 Gateway Timeout - 外部服务超时 ========== */
  case class ServiceTimeout(
    service: String,
    operation: String,
    timeoutSeconds: Int,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ExternalServiceError:
    val message   = s"Timeout calling $service.$operation after $timeoutSeconds seconds"
    val errorCode = "SERVICE_TIMEOUT"

    def httpStatus: Int = 504

  /** ========== 502 Bad Gateway - 通用外部服务错误 ========== */
  case class GeneralServiceError(
    service: String,
    operation: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ExternalServiceError:
    val message   = s"External service error: $service.$operation - ${cause.fold("")(_.getMessage)}"
    val errorCode = "EXTERNAL_SERVICE_ERROR"

    def httpStatus: Int = 502
