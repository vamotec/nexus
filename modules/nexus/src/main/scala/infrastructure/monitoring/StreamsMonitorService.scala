package app.mosia.nexus
package infrastructure.monitoring

import domain.error.AppTask
import domain.model.messaging.*
import domain.services.infra.*

import zio.*

/** Redis Streams 监控服务
  *
  * 提供以下监控功能:
  *   - Stream 健康检查
  *   - 消费者组状态监控
  *   - Pending 消息监控
  *   - Stream 自动清理（防止无限增长）
  */
final class StreamsMonitorService(redis: RedisService):

  private val streamKeys = List(
    "events:session",
    "events:training",
    "events:project",
    "events:user",
    "events:simulation"
  )

  /** 获取所有 Stream 的健康状态 */
  def getHealthStatus: AppTask[Map[String, StreamHealthStatus]] =
    ZIO.foreach(streamKeys) { streamKey =>
      getStreamHealth(streamKey).map(streamKey -> _)
    }.map(_.toMap)

  /** 获取单个 Stream 的健康状态 */
  def getStreamHealth(streamKey: String): AppTask[StreamHealthStatus] =
    for
      // 1. 获取 Stream 信息
      streamInfo <- redis.xInfoStream(streamKey).catchAll { _ =>
        ZIO.succeed(StreamInfo(0, None, None))
      }

      // 2. 获取消费者组信息
      groups <- redis.xInfoGroups(streamKey)

      // 3. 计算总 pending 消息数
      totalPending = groups.map(_.pending).sum

      // 4. 判断健康状态
      status = if streamInfo.length > 100000 then "WARNING: Too many messages"
      else if totalPending > 1000 then "WARNING: Too many pending messages"
      else if groups.isEmpty then "INFO: No consumer groups"
      else "HEALTHY"
    yield StreamHealthStatus(
      streamKey = streamKey,
      length = streamInfo.length,
      consumerGroups = groups.size,
      totalPending = totalPending,
      status = status,
      firstEntryId = streamInfo.firstEntryId,
      lastEntryId = streamInfo.lastEntryId,
      groups = groups
    )

  /** 启动定期清理任务
    *
    * 每小时清理一次，保留最近 10 万条消息
    */
  def startAutoCleanup: Task[Unit] =
    (for
//      _ <- ZIO.logInfo("Starting auto-cleanup task for Redis Streams")
      _ <- cleanupAllStreams
      _ <- ZIO.sleep(1.hour)
    yield ()).forever

  /** 清理所有 Stream */
  private def cleanupAllStreams: Task[Unit] =
    ZIO.foreachDiscard(streamKeys) { streamKey =>
      redis
        .xTrim(streamKey, maxLen = 100000, approximate = true)
        .flatMap { trimmed =>
          if trimmed > 0 then ZIO.logInfo(s"Trimmed $trimmed messages from $streamKey")
          else ZIO.unit
        }
        .catchAll { error =>
          ZIO.logWarning(s"Failed to trim $streamKey: ${error.getMessage}")
        }
    }

  /** 手动清理指定 Stream
    *
    * @param streamKey
    *   Stream 的 key
    * @param maxLen
    *   保留的最大消息数量
    */
  def trimStream(streamKey: String, maxLen: Long = 100000): AppTask[Long] =
    redis.xTrim(streamKey, maxLen)

object StreamsMonitorService:
  val live: ZLayer[RedisService, Nothing, StreamsMonitorService] =
    ZLayer.fromFunction(StreamsMonitorService(_))
