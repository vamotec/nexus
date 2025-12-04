package app.mosia.nexus
package domain.config.messaging

/** RabbitMQ 配置
  *
  * 用于外部通知（邮件、短信）的消息队列
  *
  * @param host
  *   RabbitMQ 主机地址
  * @param port
  *   RabbitMQ 端口 (默认 5672)
  * @param username
  *   用户名
  * @param password
  *   密码
  * @param virtualHost
  *   虚拟主机 (默认 "/")
  * @param exchange
  *   Exchange 名称 (用于通知事件)
  * @param queue
  *   队列名称前缀
  */
case class RabbitMQConfig(
  host: String,
  port: Int,
  username: String,
  password: String,
  virtualHost: String = "/",
  exchange: String = "nexus.notifications",
  queue: String = "nexus.notifications"
)
