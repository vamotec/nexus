package app.mosia.nexus
package domain.services.infra

import domain.error.*
import domain.model.messaging.*

import zio.*
import zio.json.*

/** Redis 服务接口
  *
  * 支持基础 KV 操作、Hash 操作和 Streams 操作
  */
trait RedisService:
  // ============================================================================
  // 基础 KV 操作
  // ============================================================================

  def get(key: String): AppTask[Option[String]]

  def set(key: String, value: String): AppTask[Unit]

  def set(key: String, value: String, ttlSeconds: Long): AppTask[Unit]

  def del(key: String): AppTask[Unit]

  def exists(key: String): AppTask[Boolean]

  def expire(key: String, seconds: Long): AppTask[Boolean]

  def incr(key: String): AppTask[Long]

  // ============================================================================
  // Hash 操作
  // ============================================================================

  def hget(key: String, field: String): AppTask[Option[String]]

  def hset(key: String, field: String, value: String): AppTask[Unit]

  def hgetAll(key: String): AppTask[Map[String, String]]

  // ============================================================================
  // Streams 操作
  // ============================================================================

  /** 添加消息到 Stream
    *
    * @param streamKey
    *   Stream 的 key
    * @param fields
    *   消息字段（通常包含 event_type, payload 等）
    * @return
    *   消息 ID (例如: "1234567890123-0")
    */
  def xAdd(streamKey: String, fields: Map[String, String]): AppTask[String]

  /** 读取 Stream 消息（单个消费者）
    *
    * @param streamKey
    *   Stream 的 key
    * @param lastId
    *   上次读取的消息 ID，使用 "0" 表示从头开始，"$" 表示仅新消息
    * @param count
    *   最多读取的消息数量
    * @return
    *   消息列表
    */
  def xRead(streamKey: String, lastId: String = "$", count: Int = 100): AppTask[List[StreamMessage[String, String]]]

  /** 创建消费者组
    *
    * @param streamKey
    *   Stream 的 key
    * @param groupName
    *   消费者组名称
    * @param startId
    *   起始 ID，使用 "0" 从头开始，"$" 仅新消息
    */
  def xGroupCreate(streamKey: String, groupName: String, startId: String = "$"): AppTask[Unit]

  /** 消费者组读取消息
    *
    * @param groupName
    *   消费者组名称
    * @param consumerName
    *   消费者名称
    * @param streamKey
    *   Stream 的 key
    * @param count
    *   最多读取的消息数量
    * @return
    *   消息列表
    */
  def xReadGroup(
    groupName: String,
    consumerName: String,
    streamKey: String,
    count: Int = 10
  ): AppTask[List[StreamMessage[String, String]]]

  /** 确认消息已处理
    *
    * @param streamKey
    *   Stream 的 key
    * @param groupName
    *   消费者组名称
    * @param messageIds
    *   要确认的消息 ID 列表
    */
  def xAck(streamKey: String, groupName: String, messageIds: List[String]): AppTask[Long]

  /** 修剪 Stream（限制长度，防止无限增长）
    *
    * @param streamKey
    *   Stream 的 key
    * @param maxLen
    *   保留的最大消息数量
    * @param approximate
    *   是否使用近似修剪（更高效）
    */
  def xTrim(streamKey: String, maxLen: Long, approximate: Boolean = true): AppTask[Long]

  /** 获取 Stream 信息（用于监控）
    *
    * @param streamKey
    *   Stream 的 key
    * @return
    *   Stream 统计信息
    */
  def xInfoStream(streamKey: String): AppTask[StreamInfo]

  /** 获取消费者组信息（用于监控）
    *
    * @param streamKey
    *   Stream 的 key
    * @return
    *   消费者组列表
    */
  def xInfoGroups(streamKey: String): AppTask[List[ConsumerGroupInfo]]
