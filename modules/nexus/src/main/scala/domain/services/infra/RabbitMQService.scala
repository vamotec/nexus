package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask

import nl.vroste.zio.amqp.*

import zio.{Scope, ZIO}

/** RabbitMQ 服务接口
  *
  * 提供与 RabbitMQ 交互的核心操作
  */
trait RabbitMQService:
  /** 创建一个已经连接的 Channel
   *
   * @return
   * ZIO[Scope, Throwable, Channel]
   */
  def getChannel: AppTask[Channel]
  
  /** 发布消息到 RabbitMQ Exchange
    *
    * @param exchange
    *   Exchange 名称
    * @param routingKey
    *   路由键
    * @param message
    *   消息内容（JSON 字符串）
    * @return
    *   Task[Unit]
    */
  def publish(exchange: String, routingKey: String, message: String): AppTask[Unit]

  /** 声明 Exchange
    *
    * @param exchange
    *   Exchange 名称
    * @param exchangeType
    *   Exchange 类型（"direct", "topic", "fanout", "headers"）
    * @param durable
    *   是否持久化
    * @return
    *   Task[Unit]
    */
  def declareExchange(exchange: String, exchangeType: String = "topic", durable: Boolean = true): AppTask[Unit]

  /** 声明队列
    *
    * @param queue
    *   队列名称
    * @param durable
    *   是否持久化
    * @param exclusive
    *   是否独占
    * @param autoDelete
    *   是否自动删除
    * @return
    *   Task[Unit]
    */
  def declareQueue(
    queue: String,
    durable: Boolean = true,
    exclusive: Boolean = false,
    autoDelete: Boolean = false
  ): AppTask[Unit]

  /** 绑定队列到 Exchange
    *
    * @param queue
    *   队列名称
    * @param exchange
    *   Exchange 名称
    * @param routingKey
    *   路由键
    * @return
    *   Task[Unit]
    */
  def bindQueue(queue: String, exchange: String, routingKey: String): AppTask[Unit]

  /** 关闭连接 */
  def close: AppTask[Unit]
