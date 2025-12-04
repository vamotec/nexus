package app.mosia.nexus
package application.dto.response.session


case class ControlEndpointResponse(
  wsUrl: String, // e.g., "ws://neuro:8765/control/session-123"
  token: String, // 24 小时有效的 JWT token
  webrtcSignalingUrl: String
)
