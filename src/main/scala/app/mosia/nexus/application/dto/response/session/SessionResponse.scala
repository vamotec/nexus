package app.mosia.nexus.application.dto.response.session

import app.mosia.nexus.application.dto.response.scene.SceneResponse
import app.mosia.nexus.application.dto.response.session.{ControlEndpointResponse, MetricsResponse, StreamEndpointResponse}
import caliban.schema.{ArgBuilder, Schema}

case class SessionResponse(
  id: String,
  userId: String,
  projectId: String,
  status: String,
  scene: SceneResponse,
  metrics: Option[MetricsResponse],
  streamEndpoint: Option[StreamEndpointResponse],
  controlEndpoint: Option[ControlEndpointResponse],
  createdAt: Long,
  startedAt: Option[Long]
) derives Schema.SemiAuto,
      ArgBuilder
