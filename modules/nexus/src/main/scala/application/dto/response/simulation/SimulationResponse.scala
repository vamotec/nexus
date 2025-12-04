package app.mosia.nexus
package application.dto.response.simulation

import application.dto.model.scene.SceneConfigDto
import domain.model.session.SessionId
import domain.model.simulation.{Simulation, SimulationId, SimulationStatus}

import java.time.Instant

import zio.json.*

case class SimulationResponse(
  simulationId: String,
  name: String,
  description: Option[String],
  sceneConfig: SceneConfigDto,
  createdAt: String
) derives JsonCodec
