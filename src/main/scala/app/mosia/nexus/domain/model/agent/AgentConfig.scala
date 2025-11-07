package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.Goal.GoalConfig
import app.mosia.nexus.domain.model.scene.Sensor

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
  goals: List[GoalConfig],

  // 元数据
  tags: Set[String],
  description: Option[String],
  version: String = "1.0"
)
