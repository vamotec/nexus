package app.mosia.nexus
package domain.model.agent

import domain.model.common.{Quaternion, Vector3D}

/** 智能体状态 */
case class AgentState(
  position: Vector3D,
  rotation: Quaternion,
  velocity: Vector3D,
  angularVelocity: Vector3D,
  health: Double = 100.0,
  fuel: Option[Double] = None, // 燃油/电量
  damage: Double = 0.0
)
