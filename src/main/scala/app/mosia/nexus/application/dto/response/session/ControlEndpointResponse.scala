package app.mosia.nexus.application.dto.response.session

import caliban.schema.{ArgBuilder, Schema}

case class ControlEndpointResponse(
  wsUrl: String,
  token: String, // 鉴权 token
) derives Schema.SemiAuto,
      ArgBuilder
