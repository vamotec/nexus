package app.mosia.nexus
package infrastructure.messaging.consumer

import application.states.OutboxStats
import domain.config.AppConfig
import domain.model.outbox.EventOutbox
import domain.repository.OutboxRepository
import domain.services.infra.RedisService

import zio.*
import zio.json.EncoderOps

import java.time.Instant

/** Outbox 处理器
  *
  * 后台轮询 Outbox 表，将待处理的事件发布到 Redis Streams
  *
  * 工作流程: 1. 每隔 N 秒轮询一次 Outbox 表 2. 查询 PENDING 状态的事件（使用 FOR UPDATE SKIP LOCKED） 3. 发布到 Redis Streams 4. 更新状态为 PUBLISHED（或 FAILED）
  *
  * 特性: - **并发安全**: 使用 FOR UPDATE SKIP LOCKED 避免多实例冲突 - **失败重试**: 指数退避重试（10s, 30s, 90s） - **失败上限**: 超过最大重试次数标记为 FAILED - **自动清理**: 定期删除已发布的旧事件
  */
final class OutboxProcessor(
  outboxRepo: OutboxRepository,
  redis: RedisService,
  config: AppConfig
):

  private val pollInterval = 1.second // 轮询间隔
  private val batchSize = 100         // 每次处理的事件数量
  private val cleanupInterval = 1.hour // 清理间隔
  private val retentionDays = 7       // 保留天数

  /** 启动 Outbox 处理器（后台运行） */
  def start: Task[Unit] = ZIO.scoped:
    for
//      _ <- ZIO.logInfo("Starting OutboxProcessor...")

      // 1. 启动事件处理循环
      _ <- processLoop.forever.forkScoped

      // 2. 启动清理任务
      _ <- processLoop.forever.forkScoped

//      _ <- ZIO.logInfo("OutboxProcessor started successfully")
    yield ()

  /** 事件处理循环 */
  private def processLoop: Task[Unit] =
    (for
      // 1. 查询待处理事件
      events <- outboxRepo.findPendingEvents(batchSize)

      // 2. 处理事件
      _ <- if events.nonEmpty then
        ZIO.logInfo(s"Processing ${events.size} outbox events...") *>
          ZIO.foreachDiscard(events)(processEvent)
      else ZIO.unit

      // 3. 休眠
      _ <- ZIO.sleep(pollInterval)
    yield ()).catchAll { error =>
      ZIO.logError(s"Error in OutboxProcessor loop: ${error.getMessage}") *>
        ZIO.sleep(pollInterval)
    }

  /** 处理单个事件 */
  private def processEvent(event: EventOutbox): Task[Unit] =
    (for
      // 1. 标记为处理中
      processingEvent <- ZIO.succeed(event.markAsProcessing)
      _ <- outboxRepo.update(processingEvent)

      // 2. 发布到 Redis Streams
      streamKey = getStreamKey(event.aggregateType)
      fields = Map(
        "event_type" -> event.eventType,
        "payload" -> event.payload.toJson,
        "timestamp" -> event.createdAt.toString,
        "aggregate_id" -> event.aggregateId
      )
      _ <- redis.xAdd(streamKey, fields)

      // 3. 标记为已发布
      publishedEvent = processingEvent.markAsPublished
      _ <- outboxRepo.update(publishedEvent)

      // 4. 记录日志
      _ <- ZIO.logInfo(
        s"Published outbox event: ${event.eventType} (id=${event.id}, aggregate=${event.aggregateId}) to $streamKey"
      )
    yield ()).catchAll { error =>
      // 处理失败：增加重试次数或标记为失败
      val failedEvent = event.markAsFailed(error.getMessage)
      outboxRepo.update(failedEvent) *>
        ZIO.logWarning(
          s"Failed to process outbox event: ${event.eventType} (id=${event.id}), retry=${failedEvent.retryCount}/${failedEvent.maxRetries}, error=${error.getMessage}"
        )
    }

  /** 清理循环 */
  private def cleanupLoop: Task[Unit] =
    (for
      _ <- ZIO.sleep(cleanupInterval)

      // 删除已发布且超过保留期的事件
      cutoffTime = Instant.now().minusSeconds(retentionDays * 24 * 3600)
      deleted <- outboxRepo.deletePublishedOlderThan(cutoffTime)

      _ <- if deleted > 0 then ZIO.logInfo(s"Cleaned up $deleted old outbox events")
      else ZIO.unit
    yield ()).catchAll { error =>
      ZIO.logError(s"Error in OutboxProcessor cleanup: ${error.getMessage}") *>
        ZIO.sleep(cleanupInterval)
    }

  /** 获取 Stream Key
    *
    * 根据聚合类型确定发布到哪个 Stream
    */
  private def getStreamKey(aggregateType: String): String =
    aggregateType.toLowerCase match
      case "session"     => "events:session"
      case "trainingjob" => "events:training"
      case "user"        => "events:user"
      case "project"     => "events:project"
      case "simulation"  => "events:simulation"
      case _             => "events:domain"

  /** 获取 Outbox 统计信息（监控用） */
  def getStats: Task[OutboxStats] =
    for
      statusCounts <- outboxRepo.countByStatus
      failedEvents <- outboxRepo.findFailedEvents(limit = 10)
    yield OutboxStats(
      pendingCount = statusCounts.getOrElse(domain.model.outbox.OutboxStatus.Pending, 0L),
      processingCount = statusCounts.getOrElse(domain.model.outbox.OutboxStatus.Processing, 0L),
      publishedCount = statusCounts.getOrElse(domain.model.outbox.OutboxStatus.Published, 0L),
      failedCount = statusCounts.getOrElse(domain.model.outbox.OutboxStatus.Failed, 0L),
      recentFailures = failedEvents
    )

object OutboxProcessor:
  val live: ZLayer[OutboxRepository & RedisService & AppConfig, Nothing, OutboxProcessor] =
    ZLayer.fromFunction(OutboxProcessor(_, _, _))
