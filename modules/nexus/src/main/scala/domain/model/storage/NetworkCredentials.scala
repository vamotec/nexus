package app.mosia.nexus
package domain.model.storage

case class NetworkCredentials(
  username: String,
  password: String, // 加密存储
  domain: Option[String] = None
)
