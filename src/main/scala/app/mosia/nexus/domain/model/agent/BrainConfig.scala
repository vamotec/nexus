package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.Control.ControlConfig
import app.mosia.nexus.domain.model.common.Learning.LearningConfig
import app.mosia.nexus.domain.model.common.Perception.PerceptionConfig
import app.mosia.nexus.domain.model.common.Planning.PlanningConfig

/** 决策系统配置 */
case class BrainConfig(
  // 感知配置
  perception: PerceptionConfig,

  // 规划配置
  planning: PlanningConfig,

  // 控制配置
  control: ControlConfig,

  // 学习配置 (RL/ML)
  learning: Option[LearningConfig],

  // 决策频率
  updateRate: Double = 10.0 // Hz
)
