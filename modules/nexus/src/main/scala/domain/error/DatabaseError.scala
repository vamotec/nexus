package app.mosia.nexus
package domain.error

import java.sql.SQLException

sealed trait DatabaseError extends ExternalServiceError:
  def httpStatus: Int = 502
  def sqlError: SQLException

  /** SQL 状态码 */
  def sqlState: String = Option(sqlError.getSQLState).getOrElse("unknown")

  /** SQL 错误码 */
  def sqlErrorCode: Int = sqlError.getErrorCode

  /** 是否是约束违反 */
  def isConstraintViolation: Boolean = sqlState.startsWith("23")

object DatabaseError:
  /** 通用数据库错误 */
  case class General(
    operation: String,
    sqlError: SQLException,
    context: Map[String, String] = Map.empty
  ) extends DatabaseError:
    val message           = s"Database error during $operation: ${sqlError.getMessage}"
    val errorCode: String = "DATABASE_ERROR"
    val cause             = Some(sqlError)

  /** 连接错误 */
  case class ConnectionError(
    sqlError: SQLException,
    context: Map[String, String] = Map.empty
  ) extends DatabaseError:
    val message           = s"Database connection error: ${sqlError.getMessage}"
    val errorCode: String = "DATABASE_CONNECTION_ERROR"
    val cause             = Some(sqlError)

  /** 查询超时 */
  case class QueryTimeout(
    operation: String,
    timeoutSeconds: Option[Int] = None,
    sqlError: SQLException,
    context: Map[String, String] = Map.empty
  ) extends DatabaseError:
    val message: String = timeoutSeconds match
      case Some(timeout) => s"Query timeout after $timeout seconds during $operation"
      case None => s"Query timeout during $operation"
    val errorCode: String = "DATABASE_QUERY_TIMEOUT"
    val cause             = Some(sqlError)

  /** 死锁 */
  case class Deadlock(
    operation: String,
    sqlError: SQLException,
    context: Map[String, String] = Map.empty
  ) extends DatabaseError:
    val message           = s"Database deadlock detected during $operation"
    val errorCode: String = "DATABASE_DEADLOCK"
    val cause             = Some(sqlError)
