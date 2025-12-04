package app.mosia.nexus
package infrastructure.messaging

import domain.error.AppTask
import domain.services.infra.{DomainEventPublisher, RedisService}

import zio.*
import zio.json.*

/** Redis Streams 事件发布器
  *
  * 将领域事件发布到 Redis Streams
  *
  * Stream 命名规则:
  *   - SessionEvent → "events:session"
  *   - TrainingEvent → "events:training"
  *   - ProjectEvent → "events:project"
  *   - UserEvent → "events:user"
  *   - SimulationEvent → "events:simulation"
  *
  * 消息格式:
  *   {
  *     "event_type": "SessionCreated",
  *     "payload": "{...json...}",
  *     "timestamp": "2024-11-26T10:30:00Z",
  *     "aggregate_id": "uuid"
  *   }
  */
final class RedisEventPublisher(redis: RedisService) extends DomainEventPublisher:

  /** 发布领域事件到 Redis Streams
    *
    * @param event
    *   领域事件（必须有 JsonEncoder 和 Tag）
    * @tparam E
    *   事件类型
    * @return
    *   Task[Unit]
    */
  override def publish[E: {JsonEncoder, Tag}](event: E): AppTask[Unit] =
    for
      // 1. 获取事件类型信息
      eventTypeName <- ZIO.succeed(getEventTypeName[E])

      // 2. 确定 Redis Stream Key（基于事件类型）
      streamKey = getStreamKeyForEvent(eventTypeName)

      // 3. 序列化事件为 JSON
      eventJson <- ZIO.succeed(event.toJson)

      // 4. 获取聚合 ID（用于分区和追踪）
      aggregateId = getAggregateId(event, eventTypeName)

      // 5. 构建消息字段
      fields = Map(
        "event_type" -> eventTypeName,
        "payload" -> eventJson,
        "timestamp" -> java.time.Instant.now().toString,
        "aggregate_id" -> aggregateId
      )

      // 6. 发布到 Redis Stream
      messageId <- redis.xAdd(streamKey, fields)

      // 7. 记录日志
      _ <- ZIO.logInfo(s"Published event: $eventTypeName to stream: $streamKey with message ID: $messageId")
    yield ()

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /** 获取事件类型名称 */
  private def getEventTypeName[E: Tag]: String =
    summon[Tag[E]].tag.shortName

  /** 根据事件类型确定 Redis Stream Key
    *
    * 命名模式: events:<domain>
    */
  private def getStreamKeyForEvent(eventTypeName: String): String =
    eventTypeName match
      // Session 事件
      case name if name.contains("SessionEvent") || name.contains("Session") =>
        "events:session"

      // Training 事件
      case name if name.contains("TrainingEvent") || name.contains("Training") =>
        "events:training"

      // Project 事件
      case name if name.contains("ProjectEvent") || name.contains("Project") =>
        "events:project"

      // User 事件
      case name if name.contains("UserEvent") || name.contains("User") =>
        "events:user"

      // Simulation 事件
      case name if name.contains("SimulationEvent") || name.contains("Simulation") =>
        "events:simulation"

      // 默认 stream
      case _ =>
        "events:domain"

  /** 获取聚合 ID（用于追踪和调试）
    *
    * 尝试从事件中提取 aggregateId
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

object RedisEventPublisher:
  /** 创建 DomainEventPublisher 的 ZLayer */
  val live: ZLayer[RedisService, Nothing, RedisEventPublisher] =
    ZLayer.fromFunction(RedisEventPublisher(_))
