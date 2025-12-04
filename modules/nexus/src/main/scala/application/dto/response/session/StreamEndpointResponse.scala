package app.mosia.nexus
package application.dto.response.session

case class StreamEndpointResponse(
  host: String,
  port: Int,
  protocol: String,
  url: String // 完整的连接 URL
)
