package app.mosia.nexus
package domain.config.database

case class DbConfig(
  dataSourceClassName: String,
  dataSource: Map[String, String],
  connectionTimeout: Long
)
