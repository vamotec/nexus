package app.mosia.nexus
package domain.model.agent

import domain.model.scene.Sensor

case class AgentConfig(
  id: AgentId,
  name: String,
  agentType: AgentType,
  controlMode: ControlMode,

  // 物理属性
  physics: AgentPhysics,

  // 传感器配置
  sensors: List[Sensor],

  // 决策系统配置
  brain: BrainConfig,

  // 行为配置
  behavior: BehaviorConfig,

  // 初始状态
  initialState: AgentState,

  // 目标设置
  goals: List[String],

  // 元数据
  tags: Set[String],
  description: Option[String],
  version: String = "1.0"
)
