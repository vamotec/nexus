package app.mosia.nexus.domain.model.task

/** 任务目标 */
case class Objective(
  id: ObjectiveId,
  description: String,
  target: ObjectiveTarget,
  priority: Int = 1,
  weight: Double = 1.0 // 在总体评估中的权重
)
