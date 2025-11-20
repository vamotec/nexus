package app.mosia.nexus
package domain.model.task

/** 任务依赖 */
case class TaskDependency(
  taskId: TaskId,
  requiredScore: Double = 0.0,
  mustComplete: Boolean = true
)
