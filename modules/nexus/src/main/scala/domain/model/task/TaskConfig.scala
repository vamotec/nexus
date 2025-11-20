package app.mosia.nexus
package domain.model.task

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 任务配置 */
case class TaskConfig(
  id: TaskId,
  name: String,
  taskType: TaskType,
  description: String,

  // 任务目标
  objectives: List[Objective],

  // 成功条件
  successCriteria: SuccessCriteria,

  // 失败条件
  failureCriteria: FailureCriteria,

  // 场景设置
  environment: TaskEnvironment,

  // 时间限制
  timeLimit: Option[Duration],

  // 难度设置
  difficulty: DifficultyLevel,

  // 评估指标
  metrics: List[EvaluationMetric],

  // 依赖关系
  dependencies: List[TaskDependency]

  // 奖励设置 (用于强化学习)
//                       rewards: Option[RewardConfig]
)
