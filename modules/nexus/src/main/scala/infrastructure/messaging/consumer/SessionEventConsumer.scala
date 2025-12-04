package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.SessionEvent
import domain.event.SessionEvent.*
import domain.model.messaging.StreamMessage
import domain.repository.SessionRepository
import domain.services.infra.RedisService

import zio.*
import zio.json.*

import java.util.UUID

/** Session 事件消费者（Redis Streams 实现）
  *
  * 从 Redis Streams 消费会话相关事件并处理
  *
  * 订阅 Stream: "events:session"
  *
  * 处理的事件:
  *   - SessionCreated: 会话创建事件
  *   - SessionStarted: 会话启动事件
  *   - SessionStopped: 会话停止事件
  */
final class SessionEventConsumer(
  redis: RedisService,
  sessionRepository: SessionRepository,
  config: AppConfig
):

  private val streamKey = "events:session"
  private val groupName = "nexus-session-consumers"
  private val consumerName = s"worker-${UUID.randomUUID().toString.take(8)}"

  /** 启动消费者
    *
    * 订阅 events:session stream 并持续消费事件
    */
  def start: Task[Unit] =
    for
      // 1. 创建消费者组（如果不存在）
      _ <- redis.xGroupCreate(streamKey, groupName, "0").catchAll { error =>
        ZIO.logWarning(s"Consumer group might already exist: ${error.getMessage}")
      }

      // 2. 记录启动日志
//      _ <- ZIO.logInfo(s"SessionEventConsumer started: group=$groupName, consumer=$consumerName")

      // 3. 启动消费循环
      _ <- consumeLoop.forever
    yield ()

  /** 消费循环 */
  private def consumeLoop: Task[Unit] =
    for
      // 1. 读取消息
      messages <- redis.xReadGroup(groupName, consumerName, streamKey, count = 10)

      // 2. 处理每条消息
      _ <- ZIO.foreachDiscard(messages) { msg =>
        processMessage(msg)
          .catchAll { error =>
            ZIO.logError(s"Failed to process message ${msg.id}: ${error.getMessage}")
          }
          .zipRight(
            // 3. 确认消息（即使处理失败也确认，避免阻塞）
            redis.xAck(streamKey, groupName, List(msg.id))
          )
      }

      // 4. 如果没有新消息，稍作休息
      _ <- if messages.isEmpty then ZIO.sleep(1.second) else ZIO.unit
    yield ()

  /** 处理单条消息 */
  private def processMessage(msg: StreamMessage[String, String]): Task[Unit] =
    for
      // 1. 提取 payload
      payloadJson <- ZIO
        .fromOption(msg.body.get("payload"))
        .orElseFail(new RuntimeException(s"Missing payload in message ${msg.id}"))

      // 2. 解析事件
      event <- ZIO
        .fromEither(payloadJson.fromJson[SessionEvent])
        .mapError(err => new RuntimeException(s"Failed to parse session event: $err"))

      // 3. 根据事件类型处理
      _ <- event match
        case SessionCreated(sessionId, userId, occurredAt) =>
          handleSessionCreated(sessionId, userId, occurredAt)

        case SessionStarted(sessionId, assignment, occurredAt) =>
          handleSessionStarted(sessionId, assignment, occurredAt)

        case SessionStopped(sessionId, reason, occurredAt) =>
          handleSessionStopped(sessionId, reason, occurredAt)

      // 4. 记录日志
      _ <- ZIO.logInfo(s"Processed session event: ${event.getClass.getSimpleName} (message: ${msg.id})")
    yield ()

  /** 处理 SessionCreated 事件 */
  private def handleSessionCreated(
    sessionId: domain.model.session.SessionId,
    userId: domain.model.user.UserId,
    occurredAt: java.time.Instant
  ): Task[Unit] =
    for _ <- ZIO.logInfo(s"Session created: $sessionId by user $userId at $occurredAt")
    // TODO: 可以在这里更新缓存、发送通知等
    yield ()

  /** 处理 SessionStarted 事件 */
  private def handleSessionStarted(
    sessionId: domain.model.session.SessionId,
    assignment: domain.model.resource.ResourceAssignment,
    occurredAt: java.time.Instant
  ): Task[Unit] =
    for
      // 1. 查询会话
      sessionOpt <- sessionRepository.findById(sessionId)

      // 2. 更新会话状态
      _ <- sessionOpt match
        case Some(session) =>
          val updatedSession = session.copy(
            status = domain.model.session.SessionStatus.Running,
            resourceAssignment = Some(assignment),
            startedAt = Some(occurredAt)
          )
          sessionRepository.update(updatedSession)

        case None =>
          ZIO.logWarning(s"Session not found: $sessionId, skipping update")

      _ <- ZIO.logInfo(s"Session started: $sessionId at $occurredAt")
    yield ()

  /** 处理 SessionStopped 事件 */
  private def handleSessionStopped(
    sessionId: domain.model.session.SessionId,
    reason: String,
    occurredAt: java.time.Instant
  ): Task[Unit] =
    for
      // 1. 查询会话
      sessionOpt <- sessionRepository.findById(sessionId)

      // 2. 更新会话状态
      _ <- sessionOpt match
        case Some(session) =>
          val updatedSession = session.copy(
            status = domain.model.session.SessionStatus.Completed,
            completedAt = Some(occurredAt)
          )
          sessionRepository.update(updatedSession)

        case None =>
          ZIO.logWarning(s"Session not found: $sessionId, skipping update")

      _ <- ZIO.logInfo(s"Session stopped: $sessionId, reason: $reason at $occurredAt")
    yield ()

object SessionEventConsumer:
  /** 创建并启动消费者 */
  val live: ZLayer[RedisService & SessionRepository & AppConfig, Nothing, SessionEventConsumer] =
    ZLayer.fromFunction(SessionEventConsumer(_, _, _))
