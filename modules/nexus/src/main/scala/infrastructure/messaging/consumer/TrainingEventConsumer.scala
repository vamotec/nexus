package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.TrainingEvent
import domain.event.TrainingEvent.*
import domain.repository.TrainingRepository
import domain.services.infra.RedisService

import app.mosia.nexus.domain.model.messaging.StreamMessage
import zio.*
import zio.json.*

import java.util.UUID

/** Training 事件消费者（Redis Streams 实现）
  *
  * 从 Redis Streams 消费训练相关事件并处理
  *
  * 订阅 Stream: "events:training"
  *
  * 处理的事件:
  *   - TrainingJobCreated: 训练任务创建事件
  *   - TrainingJobCompleted: 训练任务完成事件
  */
final class TrainingEventConsumer(
  redis: RedisService,
  trainingRepository: TrainingRepository,
  config: AppConfig
):

  private val streamKey = "events:training"
  private val groupName = "nexus-training-consumers"
  private val consumerName = s"worker-${UUID.randomUUID().toString.take(8)}"

  /** 启动消费者
    *
    * 订阅 events:training stream 并持续消费事件
    */
  def start: Task[Unit] =
    for
      // 1. 创建消费者组（如果不存在）
      _ <- redis.xGroupCreate(streamKey, groupName, "0").catchAll { error =>
        ZIO.logWarning(s"Consumer group might already exist: ${error.getMessage}")
      }

      // 2. 记录启动日志
//      _ <- ZIO.logInfo(s"TrainingEventConsumer started: group=$groupName, consumer=$consumerName")

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
        .fromEither(payloadJson.fromJson[TrainingEvent])
        .mapError(err => new RuntimeException(s"Failed to parse training event: $err"))

      // 3. 根据事件类型处理
      _ <- event match
        case TrainingJobCreated(jobId, sessionId, occurredAt) =>
          handleTrainingJobCreated(jobId, sessionId, occurredAt)

        case TrainingJobCompleted(jobId, result, occurredAt) =>
          handleTrainingJobCompleted(jobId, result, occurredAt)

      // 4. 记录日志
      _ <- ZIO.logInfo(s"Processed training event: ${event.getClass.getSimpleName} (message: ${msg.id})")
    yield ()

  /** 处理 TrainingJobCreated 事件 */
  private def handleTrainingJobCreated(
    jobId: domain.model.training.TrainingJobId,
    sessionId: domain.model.session.SessionId,
    occurredAt: java.time.Instant
  ): Task[Unit] =
    for _ <- ZIO.logInfo(s"Training job created: $jobId for session $sessionId at $occurredAt")
    // TODO: 可以在这里发送通知、更新缓存等
    yield ()

  /** 处理 TrainingJobCompleted 事件 */
  private def handleTrainingJobCompleted(
    jobId: domain.model.training.TrainingJobId,
    result: domain.model.training.TrainingResult,
    occurredAt: java.time.Instant
  ): Task[Unit] =
    for
      // 1. 查询训练任务
      jobOpt <- trainingRepository.findById(jobId)

      // 2. 更新任务状态
      _ <- jobOpt match
        case Some(job) =>
          val updatedJob = job.complete(result)
          trainingRepository.update(updatedJob)

        case None =>
          ZIO.logWarning(s"Training job not found: $jobId, skipping update")

      _ <- ZIO.logInfo(s"Training job completed: $jobId with result at $occurredAt")
    yield ()

object TrainingEventConsumer:
  /** 创建消费者 */
  val live: ZLayer[RedisService & TrainingRepository & AppConfig, Nothing, TrainingEventConsumer] =
    ZLayer.fromFunction(TrainingEventConsumer(_, _, _))

  /** 启动消费者（自动在后台运行） */
  val startConsumer: ZLayer[Scope & TrainingEventConsumer, Nothing, Unit] =
    ZLayer.fromZIO:
      for
        consumer <- ZIO.service[TrainingEventConsumer]
        _ <- consumer.start.forkScoped // 在后台运行
        _ <- ZIO.logInfo("TrainingEventConsumer started successfully")
      yield ()
