package app.mosia.nexus
package domain.config.neuro

case class WebSocketConfig(
  baseUrl: String,
  controlPath: String,
  connection: Map[String, String] = Map.empty
)
