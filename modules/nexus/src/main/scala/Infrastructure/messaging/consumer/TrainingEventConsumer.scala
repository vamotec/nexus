package app.mosia.nexus
package infrastructure.messaging.consumer

import domain.config.AppConfig
import domain.event.TrainingEvent
import domain.event.TrainingEvent.*
import domain.repository.TrainingRepository
import domain.config.kafka.KafkaConfig

import zio.*
import zio.json.*
import zio.http.*
import zio.json.ast.Json
import zio.kafka.consumer.*
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.serde.Serde
/** Training 事件消费者
  *
  * 从 Kafka 消费训练相关事件并处理
  *
  * 订阅 Topic: "training-events"
  *
  * 处理的事件:
  *   - TrainingJobCreated: 训练任务创建事件
  *   - TrainingJobCompleted: 训练任务完成事件
  */
final class TrainingEventConsumer(
  settings: ConsumerSettings,
  trainingRepository: TrainingRepository
):

  /** 启动消费者
    *
    * 订阅 training-events topic 并持续消费事件
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
                  ZIO.logError(s"Failed to process training event: ${error.getMessage}") *>
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
        .fromEither(eventJson.fromJson[TrainingEvent])
        .mapError(err => new RuntimeException(s"Failed to parse training event: $err"))

      // 2. 根据事件类型处理
      _ <- event match
        case TrainingJobCreated(jobId, sessionId, occurredAt) =>
          handleTrainingJobCreated(jobId, sessionId, occurredAt)

        case TrainingJobCompleted(jobId, result, occurredAt) =>
          handleTrainingJobCompleted(jobId, result, occurredAt)

      // 3. 记录日志
      _ <- ZIO.logInfo(s"Processed training event: ${event.getClass.getSimpleName}")
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
  /** 创建并启动消费者 */
  val startConsumer: ZLayer[Scope & TrainingRepository & AppConfig, Nothing, Unit] = ZLayer.fromZIO:
    for
      config <- ZIO.service[AppConfig]
      service <- ZIO.service[TrainingRepository]
      settings = ConsumerSettings(config.kafka.bootstrapServers)
        .withGroupId(config.kafka.consumer.groupId)
        .withProperty("security.protocol", "PLAINTEXT")
        .withProperty("enable.auto.commit", config.kafka.consumer.enableAutoCommit.toString)
        .withProperty("max.poll.records", config.kafka.consumer.maxPollRecords.toString)
        .withOffsetRetrieval(
          if config.kafka.consumer.autoOffsetReset == "earliest" then OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest)
          else OffsetRetrieval.Auto(AutoOffsetStrategy.Latest)
        )
      consumer <- ZIO.succeed(TrainingEventConsumer(settings, service))
      _ <- consumer.start.forkScoped // 在后台运行
      _ <- ZIO.logInfo("TrainingEventConsumer started successfully")
    yield ()
