package app.mosia.nexus
package domain.error

trait ClientError extends AppError:
  def shouldLogDetailed: Boolean = false

object ClientError:
  /** ========== 404 Not Found - 资源未找到 ========== */
  case class NotFound(
    entityType: String,
    identifier: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ClientError:
    val message   = s"$entityType with identifier '$identifier' not found"
    val errorCode = "NOT_FOUND"

    def httpStatus: Int = 404

  /** ========== 422 Unprocessable Entity - 语义错误 ========== */
  case class UnprocessableEntity(
    reason: String,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ClientError:
    val message   = s"Cannot process entity: $reason"
    val errorCode = "UNPROCESSABLE_ENTITY"

    def httpStatus: Int = 422

  /** ========== 429 Too Many Requests - 限流 ========== */
  case class RateLimitExceeded(
    limit: Int,
    windowSeconds: Int,
    retryAfterSeconds: Option[Int] = None,
    cause: Option[Throwable] = None,
    context: Map[String, String] = Map.empty
  ) extends ClientError:
    val message: String =
      s"Rate limit exceeded: $limit requests per $windowSeconds seconds" +
        retryAfterSeconds.map(s => s". Retry after $s seconds").getOrElse("")
    val errorCode = "RATE_LIMIT_EXCEEDED"

    def httpStatus: Int = 429
