package app.mosia.nexus.infra.config

case class GrpcConfig(
  host: String,
  port: Int,
  connection: Map[String, String],
  retry: Map[String, String],
  timeout: Map[String, String]
)
