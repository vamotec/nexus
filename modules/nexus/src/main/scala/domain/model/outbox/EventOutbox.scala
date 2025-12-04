package app.mosia.nexus
package domain.model.outbox

import zio.json.ast.Json

import java.time.Instant
import java.util.UUID

/** Outbox 事件
  *
  * 实现 Transactional Outbox Pattern，确保事件发布与数据库操作的事务一致性
  *
  * 工作流程:
  *   1. 业务逻辑在事务中同时写入业务数据和 OutboxEvent
  *   2. OutboxProcessor 后台轮询 PENDING 状态的事件
  *   3. 发布到 Redis Streams（或其他消息系统）
  *   4. 更新状态为 PUBLISHED
  */
case class EventOutbox(
  id: UUID,
  eventType: String, // 事件类型，如 "UserCreated"
  aggregateId: String, // 聚合根 ID
  aggregateType: String, // 聚合根类型，如 "User"
  payload: Json, // JSON 序列化的事件数据
  status: OutboxStatus,
  createdAt: Instant,
  processedAt: Option[Instant] = None,
  publishedAt: Option[Instant] = None,
  retryCount: Int = 0,
  maxRetries: Int = 3,
  nextRetryAt: Option[Instant] = None,
  lastError: Option[String] = None,
  partitionKey: Option[String] = None
):

  /** 标记为处理中 */
  def markAsProcessing: EventOutbox =
    copy(
      status = OutboxStatus.Processing,
      processedAt = Some(Instant.now())
    )

  /** 标记为已发布 */
  def markAsPublished: EventOutbox =
    copy(
      status = OutboxStatus.Published,
      publishedAt = Some(Instant.now())
    )

  /** 标记为失败并增加重试次数
    *
    * @param error
    *   错误信息
    * @return
    *   更新后的事件（如果超过最大重试次数，状态为 Failed）
    */
  def markAsFailed(error: String): EventOutbox =
    val newRetryCount = retryCount + 1
    val isFinalFailure = newRetryCount >= maxRetries

    copy(
      status = if isFinalFailure then OutboxStatus.Failed else OutboxStatus.Pending,
      retryCount = newRetryCount,
      lastError = Some(error),
      nextRetryAt = if isFinalFailure then None else Some(calculateNextRetry(newRetryCount))
    )

  /** 计算下次重试时间（指数退避）
    *
    * 1次重试: 10秒后 2次重试: 30秒后 3次重试: 90秒后
    */
  private def calculateNextRetry(attemptCount: Int): Instant =
    val backoffSeconds = Math.pow(3, attemptCount).toLong * 10 // 10s, 30s, 90s, ...
    Instant.now().plusSeconds(backoffSeconds)

  /** 是否可以重试 */
  def canRetry: Boolean =
    status == OutboxStatus.Pending &&
      retryCount < maxRetries &&
      nextRetryAt.forall(_.isBefore(Instant.now()))

object EventOutbox:
  /** 创建新的 Outbox 事件
    *
    * @param eventType
    *   事件类型
    * @param aggregateId
    *   聚合根 ID
    * @param aggregateType
    *   聚合根类型
    * @param payload
    *   事件 JSON 数据
    * @param partitionKey
    *   分区键（可选）
    */
  def create(
    eventType: String,
    aggregateId: String,
    aggregateType: String,
    payload: Json,
    partitionKey: Option[String] = None
  ): EventOutbox =
    EventOutbox(
      id = UUID.randomUUID(),
      eventType = eventType,
      aggregateId = aggregateId,
      aggregateType = aggregateType,
      payload = payload,
      status = OutboxStatus.Pending,
      createdAt = Instant.now(),
      partitionKey = partitionKey
    )