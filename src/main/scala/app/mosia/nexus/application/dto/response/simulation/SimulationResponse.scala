package app.mosia.nexus.application.dto.response.simulation

import app.mosia.nexus.application.dto.model.scene.SceneConfigDto
import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.simulation.{Simulation, SimulationId, SimulationStatus}
import caliban.schema.Schema as Cs
import sttp.tapir.Schema
import zio.json.JsonCodec

import java.time.Instant

case class SimulationResponse(
  simulationId: String,
  name: String,
  description: Option[String],
  sceneConfig: SceneConfigDto,
  createdAt: String
) derives JsonCodec,
      Schema,
      Cs.SemiAuto
