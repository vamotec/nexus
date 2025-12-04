package app.mosia.nexus
package application.dto.response.session

import application.dto.response.scene.SceneResponse
import application.dto.response.session.{ControlEndpointResponse, SessionMetricsResponse, StreamEndpointResponse}
import domain.model.session.SessionStatus

case class SessionResponse(
  id: String,
  userId: String,
  projectId: String,
  mode: String, // manual, training, hybrid
  status: SessionStatus,
  scene: SceneResponse,
//  metrics: Option[SessionMetricsResponse],
  streamEndpoint: Option[StreamEndpointResponse],
  controlEndpoint: Option[ControlEndpointResponse],
  createdAt: Long,
  startedAt: Option[Long]
)
