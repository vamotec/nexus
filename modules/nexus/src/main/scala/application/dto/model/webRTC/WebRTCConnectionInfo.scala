package app.mosia.nexus
package application.dto.model.webRTC

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

// WebRTC 连接信息响应
case class WebRTCConnectionInfo(
  sessionId: String,
  webrtcSignalingUrl: String,
  token: String,
  clientType: String
) derives JsonCodec,
      Schema
