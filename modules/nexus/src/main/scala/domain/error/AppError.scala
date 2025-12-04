package app.mosia.nexus
package domain.error

import java.sql.SQLException
import io.grpc.{Status, StatusException, StatusRuntimeException}
import zio.*

/** 应用错误基类 按照 HTTP 语义分为客户端错误(4xx)和服务端错误(5xx)
  */
trait AppError extends Throwable:
  def message: String
  def cause: Option[Throwable]
  def context: Map[String, String]

  override def getMessage: String  = message
  override def getCause: Throwable = cause.orNull

  /** 获取对应的 HTTP 状态码 */
  def httpStatus: Int

  /** 获取错误代码（用于客户端识别） */
  def errorCode: String

  /** 是否应该记录详细日志（5xx 错误需要详细日志，4xx 通常不需要） */
  def shouldLogDetailed: Boolean

  /** 用于日志记录的详细信息 */
  def toLogString: String =
    val contextStr =
      if context.isEmpty then ""
      else s"\nContext: ${context.map((k, v) => s"$k=$v").mkString(", ")}"
    val causeStr = cause
      .map(c => s"\nCaused by: ${c.getClass.getName}: ${c.getMessage}\n${c.getStackTrace.take(5).mkString("\n  ")}")
      .getOrElse("")
    s"[$errorCode] $message$contextStr$causeStr"

object AppError:
  /** 将任意异常转换为 AppError */
  def toAppErrorMapper(error: Throwable, operation: String = "unknown"): AppError = error match
    // 已经是 AppError，直接返回
    case app: AppError => app

    // SQL 异常
    case sql: SQLException =>
      convertSqlException(sql, operation)

    // gRPC 异常
    case grpc: StatusException =>
      convertGrpcException(grpc, operation)

    case grpc: StatusRuntimeException =>
      convertGrpcRuntimeException(grpc, operation)

    // JSON 解析错误 -> 4xx
    case json: zio.json.JsonDecoder.UnsafeJson =>
      ValidationError.InvalidInput(
        field = "json",
        reason = json.getMessage,
        cause = Some(json)
      )

    // 超时错误 -> 5xx
    case timeout: java.util.concurrent.TimeoutException =>
      ExternalServiceError.ServiceTimeout(
        service = "unknown",
        operation = operation,
        timeoutSeconds = 30, // 默认值
        cause = Some(timeout)
      )

    // IO 错误 -> 5xx
    case io: java.io.IOException =>
      InternalError.UnexpectedError(
        cause = Some(io),
        context = Map("operation" -> operation)
      )

    // 其他未知错误 -> 5xx
    case other =>
      InternalError.UnexpectedError(
        cause = Some(other),
        context = Map("operation" -> operation)
      )

  /** 转换 SQL 异常 */
  private def convertSqlException(sql: SQLException, operation: String): AppError =
    val sqlState = Option(sql.getSQLState).getOrElse("unknown")
    val message  = sql.getMessage

    sqlState match
      // Class 08 - 连接错误 -> 5xx
      case state if state.startsWith("08") =>
        DatabaseError.ConnectionError(
          sqlError = sql,
          context = Map("operation" -> operation)
        )

      // Class 40 - 事务回滚 (死锁等) -> 5xx
      case "40001" | "40P01" => // serialization_failure, deadlock_detected
        DatabaseError.Deadlock(
          operation = operation,
          sqlError = sql,
          context = Map("sqlState" -> sqlState)
        )

      // Class 23 - 完整性约束违反 -> 4xx
      case "23505" => // unique_violation
        parseUniqueViolation(sql, operation)

      case "23503" => // foreign_key_violation
        parseForeignKeyViolation(sql, operation)

      case "23502" => // not_null_violation
        parseNotNullViolation(sql, operation)

      case "23514" => // check_violation
        parseCheckViolation(sql, operation)

      // Class 22 - 数据异常 -> 4xx
      case state if state.startsWith("22") =>
        ValidationError.InvalidInput(
          field = "unknown",
          reason = s"Data exception: $message",
          cause = Some(sql),
          context = Map("sqlState" -> sqlState, "operation" -> operation)
        )

      // 查询超时 -> 5xx
      case state if message.toLowerCase.contains("timeout") =>
        DatabaseError.QueryTimeout(
          operation = operation,
          sqlError = sql,
          context = Map("sqlState" -> sqlState)
        )

      // 其他数据库错误 -> 5xx
      case _ =>
        DatabaseError.General(
          operation = operation,
          sqlError = sql,
          context = Map("sqlState" -> sqlState, "errorCode" -> sql.getErrorCode.toString)
        )

  /** 解析唯一约束违反 -> 4xx Conflict */
  private def parseUniqueViolation(sql: SQLException, operation: String): ConflictError =
    val message           = sql.getMessage
    val constraintPattern = """constraint "([^"]+)"""".r
    val keyPattern        = """Key \(([^)]+)\)=\(([^)]+)\)""".r

    val constraint = constraintPattern.findFirstMatchIn(message).map(_.group(1))
    val keyInfo    = keyPattern.findFirstMatchIn(message).map { m =>
      (m.group(1), m.group(2))
    }

    keyInfo match
      case Some((field, value)) =>
        val entityType = constraint.flatMap(extractEntityType).getOrElse("Entity")
        ConflictError.DuplicateEntity(
          entityType = entityType,
          identifier = value,
          constraintName = constraint,
          cause = Some(sql),
          context = Map(
            "field" -> field,
            "operation" -> operation,
            "sqlState" -> sql.getSQLState
          )
        )
      case None =>
        // 无法解析，使用通用冲突错误
        ConflictError.DuplicateEntity(
          entityType = "Entity",
          identifier = "unknown",
          constraintName = constraint,
          cause = Some(sql),
          context = Map("operation" -> operation)
        )

  /** 解析外键约束违反 -> 4xx Validation */
  private def parseForeignKeyViolation(sql: SQLException, operation: String): ValidationError =
    val message         = sql.getMessage
    val tablePattern    = """table "([^"]+)"""".r
    val keyPattern      = """Key \(([^)]+)\)=\(([^)]+)\)""".r
    val refTablePattern = """table "([^"]+)"\.""".r

    val table    = tablePattern.findFirstMatchIn(message).map(_.group(1))
    val keyInfo  = keyPattern.findFirstMatchIn(message).map { m => (m.group(1), m.group(2)) }
    val refTable = refTablePattern.findAllMatchIn(message).toList.lastOption.map(_.group(1))

    keyInfo match
      case Some((field, value)) =>
        ValidationError.InvalidReference(
          entityType = table.map(_.capitalize).getOrElse("Entity"),
          field = field,
          referencedType = refTable.map(_.capitalize).getOrElse("referenced entity"),
          referencedId = value,
          cause = Some(sql),
          context = Map("operation" -> operation, "sqlState" -> sql.getSQLState)
        )
      case None =>
        ValidationError.InvalidInput(
          field = "unknown",
          reason = s"Foreign key constraint violation: $message",
          cause = Some(sql),
          context = Map("operation" -> operation)
        )

  /** 解析非空约束违反 -> 4xx Validation */
  private def parseNotNullViolation(sql: SQLException, operation: String): ValidationError =
    val message       = sql.getMessage
    val columnPattern = """column "([^"]+)"""".r
    val tablePattern  = """relation "([^"]+)"""".r

    val column = columnPattern.findFirstMatchIn(message).map(_.group(1))
    val table  = tablePattern.findFirstMatchIn(message).map(_.group(1))

    column match
      case Some(field) =>
        ValidationError.MissingRequiredField(
          entityType = table.map(_.capitalize).getOrElse("Entity"),
          field = field,
          cause = Some(sql),
          context = Map("operation" -> operation, "sqlState" -> sql.getSQLState)
        )
      case None =>
        ValidationError.InvalidInput(
          field = "unknown",
          reason = s"Not null constraint violation: $message",
          cause = Some(sql),
          context = Map("operation" -> operation)
        )

  /** 解析检查约束违反 -> 4xx Validation */
  private def parseCheckViolation(sql: SQLException, operation: String): ValidationError =
    val message           = sql.getMessage
    val constraintPattern = """constraint "([^"]+)"""".r
    val constraint        = constraintPattern.findFirstMatchIn(message).map(_.group(1))

    ValidationError.BusinessRuleViolation(
      rule = constraint.getOrElse("CHECK constraint"),
      details = message,
      cause = Some(sql),
      context = Map("operation" -> operation, "sqlState" -> sql.getSQLState)
    )

  /** 转换 gRPC 异常 */
  private def convertGrpcException(grpc: StatusException, operation: String): AppError =
    val status = grpc.getStatus
    convertGrpcStatus(status, operation, Some(grpc))

  private def convertGrpcRuntimeException(grpc: StatusRuntimeException, operation: String): AppError =
    val status = grpc.getStatus
    convertGrpcStatus(status, operation, Some(grpc))

  private def convertGrpcStatus(status: Status, operation: String, cause: Option[Throwable]): AppError =
    status.getCode match
      // 4xx 错误
      case Status.Code.INVALID_ARGUMENT =>
        ValidationError.InvalidInput(
          field = "grpc_request",
          reason = Option(status.getDescription).getOrElse("Invalid argument"),
          cause = cause,
          context = Map("operation" -> operation)
        )

      case Status.Code.NOT_FOUND =>
        ClientError.NotFound(
          entityType = "Resource",
          identifier = "unknown",
          cause = cause,
          context = Map("operation" -> operation)
        )

      case Status.Code.ALREADY_EXISTS =>
        ConflictError.DuplicateEntity(
          entityType = "Resource",
          identifier = "unknown",
          cause = cause,
          context = Map("operation" -> operation)
        )

      case Status.Code.PERMISSION_DENIED =>
        AuthorizationError.PermissionDenied(
          action = operation,
          resource = "unknown",
          cause = cause
        )

      case Status.Code.UNAUTHENTICATED =>
        AuthenticationError.Unauthenticated(
          reason = Option(status.getDescription).getOrElse("No credentials provided"),
          cause = cause
        )

      case Status.Code.FAILED_PRECONDITION =>
        ConflictError.StateConflict(
          entityType = "Resource",
          currentState = "unknown",
          attemptedAction = operation,
          cause = cause
        )

      case Status.Code.OUT_OF_RANGE =>
        ValidationError.InvalidInput(
          field = "range",
          reason = Option(status.getDescription).getOrElse("Value out of range"),
          cause = cause
        )

      // 5xx 错误
      case Status.Code.UNAVAILABLE =>
        ExternalServiceError.ComputeCenterUnavailable(
          centerName = "grpc_service",
          operation = operation,
          cause = Some(cause.getOrElse(new Exception(status.getDescription))),
          context = Map("grpcStatus" -> status.getCode.toString)
        )

      case Status.Code.DEADLINE_EXCEEDED =>
        ExternalServiceError.ServiceTimeout(
          service = "grpc_service",
          operation = operation,
          timeoutSeconds = 30,
          cause = cause,
          context = Map("grpcStatus" -> status.getCode.toString)
        )

      case Status.Code.RESOURCE_EXHAUSTED =>
        InternalError.ResourceExhausted(
          resource = "grpc_service",
          details = Option(status.getDescription).getOrElse("Resource exhausted"),
          cause = cause
        )

      case _ =>
        ExternalServiceError.GrpcServiceError(
          service = "unknown",
          method = operation,
          status = status,
          cause = cause
        )

  /** 从约束名称中提取实体类型 */
  private def extractEntityType(constraintName: String): Option[String] =
    constraintName.split("_").headOption.map(_.capitalize)

  extension (error: AppError)
    def toErrorResponse: ErrorResponse = ErrorResponse.from(error)
