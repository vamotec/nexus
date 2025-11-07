package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.{Quaternion, Vector3}

/** 智能体状态 */
case class AgentState(
  position: Vector3,
  rotation: Quaternion,
  velocity: Vector3,
  angularVelocity: Vector3,
  health: Double = 100.0,
  fuel: Option[Double] = None, // 燃油/电量
  damage: Double = 0.0
)
