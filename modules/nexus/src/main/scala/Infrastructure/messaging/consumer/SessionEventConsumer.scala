package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.SessionEvent
import domain.event.SessionEvent.*
import domain.repository.SessionRepository
import domain.config.kafka.KafkaConfig

import zio.*
import zio.json.*
import zio.http.*
import zio.json.ast.Json
import zio.kafka.consumer.*
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.serde.Serde
/** Session 事件消费者
  *
  * 从 Kafka 消费会话相关事件并处理
  *
  * 订阅 Topic: "session-events"
  *
  * 处理的事件:
  *   - SessionCreated: 会话创建事件
  *   - SessionStarted: 会话启动事件
  *   - SessionStopped: 会话停止事件
  */
final class SessionEventConsumer(
  settings: ConsumerSettings,
  sessionRepository: SessionRepository
):

  /** 启动消费者
    *
    * 订阅 session-events topic 并持续消费事件
    */
  def start: Task[Unit] =
    ZIO.scoped:
      Consumer
        .make(settings) // 确保有 consumerSettings 定义
        .flatMap { consumer =>
          consumer
            .plainStream(Subscription.topics("training-events"), Serde.string, Serde.string)
            .mapZIO { record =>
              processEvent(record.value)
                .catchAll { error =>
                  ZIO.logError(s"Failed to process session event: ${error.getMessage}") *>
                    ZIO.logError(s"Event payload: ${record.value}")
                } *>
                record.offset.commit // 手动提交偏移量，确保消息被确认
            }
            .runDrain
        }
        .orDie // 或者根据需求调整错误处理策略

  /** 处理单个事件 */
  private def processEvent(eventJson: String): Task[Unit] =
    for
      // 1. 解析事件
      event <- ZIO
        .fromEither(eventJson.fromJson[SessionEvent])
        .mapError(err => new RuntimeException(s"Failed to parse session event: $err"))

      // 2. 根据事件类型处理
      _ <- event match
        case SessionCreated(sessionId, userId, occurredAt) =>
          handleSessionCreated(sessionId, userId, occurredAt)

        case SessionStarted(sessionId, assignment, occurredAt) =>
          handleSessionStarted(sessionId, assignment, occurredAt)

        case SessionStopped(sessionId, reason, occurredAt) =>
          handleSessionStopped(sessionId, reason, occurredAt)

      // 3. 记录日志
      _ <- ZIO.logInfo(s"Processed session event: ${event.getClass.getSimpleName}")
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
  val startConsumer: ZLayer[Scope & SessionRepository & AppConfig, Nothing, Unit] = ZLayer.fromZIO:
    for
      config <- ZIO.service[AppConfig]
      service <- ZIO.service[SessionRepository]
      settings = ConsumerSettings(config.kafka.bootstrapServers)
        .withGroupId(config.kafka.consumer.groupId)
        .withProperty("security.protocol", "PLAINTEXT")
        .withProperty("enable.auto.commit", config.kafka.consumer.enableAutoCommit.toString)
        .withProperty("max.poll.records", config.kafka.consumer.maxPollRecords.toString)
        .withOffsetRetrieval(
          if config.kafka.consumer.autoOffsetReset == "earliest" then OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)
          else OffsetRetrieval.Auto(AutoOffsetStrategy.Latest)
        )
      consumer <- ZIO.succeed(SessionEventConsumer(settings, service))
      _ <- consumer.start.forkScoped // 在后台运行
      _ <- ZIO.logInfo("SessionEventConsumer started successfully")
    yield ()
