package app.mosia.nexus
package domain.model.simulation

import domain.model.agent.{AgentConfig, AgentId}
import domain.model.resource.ResourceRequirements
import domain.model.scene.*

import java.util.UUID

case class ConfigOverride(
  scene: Option[SceneConfig] = None,
  physics: Option[PhysicsConfig] = None,
  sensorOverrides: Map[String, SensorConfig] = Map.empty,
  agentOverrides: Map[AgentId, AgentConfig] = Map.empty,
  environment: Option[Environment] = None,
  additionalRequirements: Option[ResourceRequirements] = None
)
