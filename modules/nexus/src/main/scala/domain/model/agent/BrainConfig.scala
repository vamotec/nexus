package app.mosia.nexus
package domain.model.agent

import domain.model.common.Control.ControlConfig
import domain.model.common.Learning.LearningConfig
import domain.model.common.Perception.PerceptionConfig
import domain.model.common.Planning.PlanningConfig

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
