package app.mosia.nexus.domain.model.session

import app.mosia.nexus.domain.model.scene.SceneConfig
import app.mosia.nexus.domain.model.simulation.{SimulationId, SimulationParams}

import java.time.Instant

case class SimulationConfigSnapshot(
  simulationId: SimulationId,
  simulationName: String,
  version: String,
  sceneConfig: SceneConfig,
  simulationParams: SimulationParams,
  snapshotAt: Instant
)
