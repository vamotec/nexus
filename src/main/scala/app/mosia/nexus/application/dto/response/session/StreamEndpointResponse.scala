package app.mosia.nexus.application.dto.response.session

import caliban.schema.{ArgBuilder, Schema}

case class StreamEndpointResponse(
  host: String,
  port: Int,
  protocol: String,
  url: String, // 完整的连接 URL
) derives Schema.SemiAuto,
      ArgBuilder
