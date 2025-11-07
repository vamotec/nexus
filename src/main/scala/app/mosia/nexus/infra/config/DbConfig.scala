package app.mosia.nexus.infra.config

case class DbConfig(
  dataSourceClassName: String,
  dataSource: Map[String, String],
  connectionTimeout: Long
)
