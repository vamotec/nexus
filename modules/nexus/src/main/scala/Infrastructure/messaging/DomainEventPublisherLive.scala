package app.mosia.nexus
package infrastructure.messaging

import domain.error.AppTask
import domain.services.infra.{DomainEventPublisher, KafkaProducerService}

import zio.*
import zio.json.*
import zio.http.*
import zio.json.ast.Json

/** DomainEventPublisher 实现
  *
  * 将领域事件发布到 Kafka Topic
  *
  * Topic 命名规则:
  *   - SessionEvent → "session-events"
  *   - TrainingEvent → "training-events"
  *   - ProjectEvent → "project-events"
  *   - UserEvent → "user-events"
  */
final class DomainEventPublisherLive(kafkaProducer: KafkaProducerService) extends DomainEventPublisher:

  /** 发布领域事件到 Kafka
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

      // 2. 确定 Kafka Topic（基于事件类型）
      topic = getTopicForEvent(eventTypeName)

      // 3. 生成事件 Key（用于分区）
      key = getEventKey(event, eventTypeName)

      // 4. 发布到 Kafka
      _ <- kafkaProducer.publish(topic, key, event)

      // 5. 记录日志
      _ <- ZIO.logInfo(s"Published event: $eventTypeName to topic: $topic with key: $key")
    yield ()

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /** 获取事件类型名称 */
  private def getEventTypeName[E: Tag]: String =
    summon[Tag[E]].tag.shortName

  /** 根据事件类型确定 Kafka Topic */
  private def getTopicForEvent(eventTypeName: String): String =
    eventTypeName match
      // Session 事件
      case name if name.contains("SessionEvent") || name.contains("Session") =>
        "session-events"

      // Training 事件
      case name if name.contains("TrainingEvent") || name.contains("Training") =>
        "training-events"

      // Project 事件
      case name if name.contains("ProjectEvent") || name.contains("Project") =>
        "project-events"

      // User 事件
      case name if name.contains("UserEvent") || name.contains("User") =>
        "user-events"

      // Simulation 事件
      case name if name.contains("SimulationEvent") || name.contains("Simulation") =>
        "simulation-events"

      // 默认 topic
      case _ =>
        "domain-events"

  /** 生成事件 Key（用于 Kafka 分区）
    *
    * 尝试从事件中提取 aggregateId 作为 key
    */
  private def getEventKey[E](event: E, eventTypeName: String): String =
    // 使用反射获取 aggregateId（如果存在）
    try
      val method      = event.getClass.getMethod("aggregateId")
      val aggregateId = method.invoke(event)
      aggregateId.toString
    catch
      case _: Throwable =>
        // 如果没有 aggregateId，使用事件类型名称
        eventTypeName

object DomainEventPublisherLive:
  val live: ZLayer[KafkaProducerService, Nothing, DomainEventPublisher] =
    ZLayer.fromFunction(DomainEventPublisherLive(_))
