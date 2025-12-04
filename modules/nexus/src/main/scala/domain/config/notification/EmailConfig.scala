package app.mosia.nexus
package domain.config.notification

/** SMTP 邮件配置
  *
  * @param host
  *   SMTP 服务器地址
  * @param port
  *   SMTP 端口
  * @param username
  *   SMTP 用户名
  * @param password
  *   SMTP 密码
  * @param from
  *   发件人邮箱
  * @param fromName
  *   发件人名称
  * @param useTls
  *   是否使用 TLS
  */
case class EmailConfig(
  host: String,
  port: Int,
  username: String,
  password: String,
  from: String,
  fromName: String = "Nexus",
  useTls: Boolean = true
)
