package app.mosia.nexus.infra.config

case class WSConfig(
  baseUrl: String,
  controlPath: String,
  connection: Map[String, String]
)
