package app.mosia.nexus
package infrastructure.redis

import domain.config.AppConfig
import domain.error.*
import domain.model.messaging.*
import domain.services.infra.RedisService

import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.models.stream.PendingMessages
import io.lettuce.core.{RedisClient, XGroupCreateArgs, XReadArgs, XTrimArgs, Consumer as LettuceConsumer}
import zio.{ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

private final class LettuceRedis(cmd: RedisCommands[String, String]) extends RedisService:

  // ============================================================================
  // 基础 KV 操作
  // ============================================================================

  override def get(key: String): AppTask[Option[String]] =
    ZIO.attempt(Option(cmd.get(key))).mapError(toAppError)

  override def set(key: String, value: String): AppTask[Unit] =
    ZIO.attempt(cmd.set(key, value)).unit.mapError(toAppError)

  override def set(key: String, value: String, ttlSeconds: Long): AppTask[Unit] =
    ZIO.attempt(cmd.setex(key, ttlSeconds, value)).unit.mapError(toAppError)

  override def del(key: String): AppTask[Unit] =
    ZIO.attempt(cmd.del(key)).unit.mapError(toAppError)

  override def exists(key: String): AppTask[Boolean] =
    ZIO.attempt(cmd.exists(key) > 0).mapError(toAppError)

  override def expire(key: String, seconds: Long): AppTask[Boolean] =
    ZIO.attempt(cmd.expire(key, seconds).booleanValue()).mapError(toAppError)

  override def incr(key: String): AppTask[Long] =
    ZIO.attempt(cmd.incr(key).longValue()).mapError(toAppError)

  // ============================================================================
  // Hash 操作
  // ============================================================================

  override def hget(key: String, field: String): AppTask[Option[String]] =
    ZIO.attempt(Option(cmd.hget(key, field))).mapError(toAppError)

  override def hset(key: String, field: String, value: String): AppTask[Unit] =
    ZIO.attempt(cmd.hset(key, field, value)).unit.mapError(toAppError)

  override def hgetAll(key: String): AppTask[Map[String, String]] =
    ZIO.attempt {
      cmd.hgetall(key).asScala.toMap
    }.mapError(toAppError)

  // ============================================================================
  // Streams 操作
  // ============================================================================

  override def xAdd(streamKey: String, fields: Map[String, String]): AppTask[String] =
    ZIO.attempt {
      val javaMap = fields.asJava
      cmd.xadd(streamKey, javaMap)
    }.mapError(toAppError)

  override def xRead(streamKey: String, lastId: String = "$", count: Int = 100): AppTask[List[StreamMessage[String, String]]] =
    ZIO.attempt {
      val args = XReadArgs.Builder.count(count)
      val messages = cmd.xread(args, XReadArgs.StreamOffset.from(streamKey, lastId))

      if messages == null || messages.isEmpty then List.empty
      else messages.asScala.map { lettuceMsg =>
        StreamMessage(
          stream = lettuceMsg.getStream, 
          id = lettuceMsg.getId,
          body = lettuceMsg.getBody.asScala.toMap,
          millisElapsedFromDelivery = Option(lettuceMsg.getMillisElapsedFromDelivery).map(_.longValue),
          deliveredCount = Option(lettuceMsg.getDeliveredCount).map(_.longValue)
        )
      }.toList
    }.mapError(toAppError)

  override def xGroupCreate(streamKey: String, groupName: String, startId: String = "$"): AppTask[Unit] =
    ZIO.attempt {
      // 创建 Stream（如果不存在）
      val createArgs = XGroupCreateArgs.Builder.mkstream()
      cmd.xgroupCreate(XReadArgs.StreamOffset.from(streamKey, startId), groupName, createArgs)
    }.unit.catchAll { error =>
      // 如果消费者组已存在，忽略错误
      if error.getMessage != null && error.getMessage.contains("BUSYGROUP") then ZIO.unit
      else ZIO.fail(toAppError(error))
    }

  override def xReadGroup(
    groupName: String,
    consumerName: String,
    streamKey: String,
    count: Int = 10
  ): AppTask[List[StreamMessage[String, String]]] =
    ZIO.attempt {
      val consumer = LettuceConsumer.from(groupName, consumerName)
      val args = XReadArgs.Builder.count(count)
      val messages = cmd.xreadgroup(consumer, args, XReadArgs.StreamOffset.lastConsumed(streamKey))

      if messages == null || messages.isEmpty then List.empty
      else
        messages.asScala.map { lettuceMsg =>
          StreamMessage(
            stream = lettuceMsg.getStream,
            id = lettuceMsg.getId,
            body = lettuceMsg.getBody.asScala.toMap,
            millisElapsedFromDelivery = Option(lettuceMsg.getMillisElapsedFromDelivery).map(_.longValue),
            deliveredCount = Option(lettuceMsg.getDeliveredCount).map(_.longValue)
          )
        }.toList
    }.mapError(toAppError)

  override def xAck(streamKey: String, groupName: String, messageIds: List[String]): AppTask[Long] =
    ZIO.attempt {
      cmd.xack(streamKey, groupName, messageIds*).longValue()
    }.mapError(toAppError)

  override def xTrim(streamKey: String, maxLen: Long, approximate: Boolean = true): AppTask[Long] =
    ZIO.attempt {
      if approximate then
        cmd.xtrim(streamKey, XTrimArgs.Builder.maxlen(maxLen).approximateTrimming()).longValue()
      else
        cmd.xtrim(streamKey, XTrimArgs.Builder.maxlen(maxLen).exactTrimming()).longValue()
    }.mapError(toAppError)

  override def xInfoStream(streamKey: String): AppTask[StreamInfo] =
    ZIO.attempt {
      val info = cmd.xinfoStream(streamKey)

      // 将 List[Object] 转换为 Map
      val infoMap = info.asScala.grouped(2).collect {
        case Seq(key: String, value) => key -> value
      }.toMap

      def getLong(key: String): Long =
        infoMap.get(key).flatMap {
          case v: java.lang.Long => Some(v.longValue())
          case v: java.lang.Integer => Some(v.longValue())
          case _ => None
        }.getOrElse(0L)

      def getFirstId(key: String): Option[String] =
        infoMap.get(key).flatMap {
          case list: java.util.List[_] if !list.isEmpty =>
            list.get(0) match {
              case id: String => Some(id)
              case _ => None
            }
          case _ => None
        }

      StreamInfo(
        length = getLong("length"),
        firstEntryId = getFirstId("first-entry"),
        lastEntryId = getFirstId("last-entry")
      )
    }.mapError(toAppError)

  override def xInfoGroups(streamKey: String): AppTask[List[ConsumerGroupInfo]] =
    ZIO.attempt {
      val groups = cmd.xinfoGroups(streamKey)

      groups.asScala.flatMap { groupObj =>
        try {
          val groupInfo = groupObj.asInstanceOf[java.util.List[?]]
          val infoMap = groupInfo.asScala.grouped(2).collect {
            case Seq(key: String, value) => key -> value
          }.toMap

          Some(ConsumerGroupInfo(
            name = infoMap.get("name").map(_.toString).getOrElse(""),
            consumers = infoMap.get("consumers").flatMap {
              case v: java.lang.Long => Some(v.longValue())
              case v: java.lang.Integer => Some(v.longValue())
              case _ => None
            }.getOrElse(0L),
            pending = infoMap.get("pending").flatMap {
              case v: java.lang.Long => Some(v.longValue())
              case v: java.lang.Integer => Some(v.longValue())
              case _ => None
            }.getOrElse(0L),
            lastDeliveredId = infoMap.get("last-delivered-id").map(_.toString).getOrElse("0-0")
          ))
        } catch {
          case _: ClassCastException => None // 跳过解析失败的项
        }
      }.toList
    }.catchAll { error =>
      if error.getMessage != null &&
        (error.getMessage.contains("no such key") || error.getMessage.contains("NOGROUP"))
      then ZIO.succeed(List.empty)
      else ZIO.fail(toAppError(error))
    }

object LettuceRedis:
  /** 从环境变量读取配置的 live 实现 */
  val live: ZLayer[AppConfig, Throwable, RedisService] = ZLayer.scoped:
    for
      config <- ZIO.service[AppConfig]
      client <- ZIO.acquireRelease(
        ZIO.attempt(RedisClient.create(config.cache.redis.uri))
      )(c => ZIO.succeed(c.shutdown()))
      conn <- ZIO.acquireRelease(
        ZIO.attempt(client.connect())
      )(c => ZIO.succeed(c.close()))
    yield LettuceRedis(conn.sync())