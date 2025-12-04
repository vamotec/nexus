package app.mosia.nexus
package infrastructure.messaging

import domain.error.AppTask
import domain.model.outbox.EventOutbox
import domain.repository.OutboxRepository
import domain.services.infra.DomainEventPublisher

import app.mosia.nexus.domain.error.*
import zio.*
import zio.json.*

/** PostgreSQL Outbox 事件发布器
  *
  * 实现 Transactional Outbox Pattern: - 不直接发布事件到消息系统 - 而是在同一事务中写入 Outbox 表 - 后台 OutboxProcessor 异步发布
  *
  * 优势: - **事务一致性**: 保证业务数据和事件发布的原子性 - **可靠性**: 即使消息系统暂时不可用，事件也不会丢失 - **顺序保证**: 按照创建时间顺序处理
  *
  * 适用场景: - 用户注册（需要同时创建用户 + 发送欢迎邮件） - 支付处理（需要同时扣款 + 发送收据） - 订单创建（需要同时创建订单 + 扣减库存）
  *
  * @param outboxRepo
  *   Outbox 仓储
  */
final class PostgresOutboxPublisher(outboxRepo: OutboxRepository) extends DomainEventPublisher:

  /** 发布事件到 Outbox 表（事务中调用）
    *
    * 注意: 此方法必须在数据库事务内调用，确保业务数据和 Outbox 事件原子性提交
    *
    * @param event
    *   领域事件
    * @tparam E
    *   事件类型
    * @return
    *   Task[Unit]
    */
  override def publish[E: {JsonEncoder, Tag}](event: E): AppTask[Unit] =
    for
      // 1. 获取事件类型信息
      eventTypeName <- ZIO.succeed(getEventTypeName[E])

      // 2. 序列化事件为 JSON AST
      eventJson <- ZIO.fromEither(event.toJsonAST)
        .mapError(err => SerializationError("event", value = Some(err)))

      // 3. 提取聚合信息
      aggregateId = getAggregateId(event, eventTypeName)
      aggregateType = getAggregateType(eventTypeName)

      // 4. 创建 Outbox 事件
      outboxEvent = EventOutbox.create(
        eventType = eventTypeName,
        aggregateId = aggregateId,
        aggregateType = aggregateType,
        payload = eventJson,
        partitionKey = Some(aggregateId) // 使用聚合 ID 作为分区键
      )

      // 5. 保存到 Outbox 表（同一事务）
      _ <- outboxRepo.save(outboxEvent)

      // 6. 记录日志
      _ <- ZIO.logDebug(s"Saved event to Outbox: $eventTypeName (id=${outboxEvent.id}, aggregate=$aggregateId)")
    yield ()

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /** 获取事件类型名称 */
  private def getEventTypeName[E: Tag]: String =
    summon[Tag[E]].tag.shortName

  /** 提取聚合 ID
    *
    * 尝试通过反射获取 aggregateId 方法
    */
  private def getAggregateId[E](event: E, eventTypeName: String): String =
    try
      val method = event.getClass.getMethod("aggregateId")
      val aggregateId = method.invoke(event)
      aggregateId.toString
    catch
      case _: Throwable =>
        // 如果没有 aggregateId，使用事件类型名称
        eventTypeName

  /** 提取聚合类型
    *
    * 从事件类型名称推断聚合类型 例如: SessionCreated -> Session UserRegistered -> User
    */
  private def getAggregateType(eventTypeName: String): String =
    eventTypeName match
      case name if name.startsWith("Session")    => "Session"
      case name if name.startsWith("Training")   => "TrainingJob"
      case name if name.startsWith("User")       => "User"
      case name if name.startsWith("Project")    => "Project"
      case name if name.startsWith("Simulation") => "Simulation"
      case _ => "Unknown"

object PostgresOutboxPublisher:
  val live: ZLayer[OutboxRepository, Nothing, PostgresOutboxPublisher] =
    ZLayer.fromFunction(PostgresOutboxPublisher(_))
