package app.mosia.nexus
package application.dto.response.session

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ControlEndpointResponse(
  wsUrl: String, // e.g., "ws://neuro:8765/control/session-123"
  token: String, // 24 小时有效的 JWT token
  webrtcSignalingUrl: String
) derives Cs.SemiAuto,
      ArgBuilder
