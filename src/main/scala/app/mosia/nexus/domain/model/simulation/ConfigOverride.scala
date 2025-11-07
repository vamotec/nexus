package app.mosia.nexus.domain.model.simulation

import app.mosia.nexus.domain.model.agent.{AgentConfig, AgentId}
import app.mosia.nexus.domain.model.resource.ResourceRequirements
import app.mosia.nexus.domain.model.scene.{Environment, PhysicsConfig, SceneConfig, SensorConfig, SensorId}

case class ConfigOverride(
  scene: Option[SceneConfig] = None,
  physics: Option[PhysicsConfig] = None,
  sensorOverrides: Map[SensorId, SensorConfig] = Map.empty,
  agentOverrides: Map[AgentId, AgentConfig] = Map.empty,
  environment: Option[Environment] = None,
  additionalRequirements: Option[ResourceRequirements] = None
)
