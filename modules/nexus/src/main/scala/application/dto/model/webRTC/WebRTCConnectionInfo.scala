package app.mosia.nexus
package application.dto.model.webRTC

import sttp.tapir.Schema
import zio.json.*

// WebRTC 连接信息响应
case class WebRTCConnectionInfo(
  sessionId: String,
  webrtcSignalingUrl: String,
  token: String,
  clientType: String
) derives JsonCodec,
      Schema
