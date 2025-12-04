package app.mosia.nexus
package application.dto.response.session

import application.dto.response.scene.SceneResponse
import application.dto.response.session.{ControlEndpointResponse, SessionMetricsResponse, StreamEndpointResponse}
import domain.model.session.SessionStatus

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

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
) derives Cs.SemiAuto,
      ArgBuilder
