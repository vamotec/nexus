package app.mosia.nexus.domain.model.task

import zio.Duration

/** 成功标准 */
case class SuccessCriteria(
  minScore: Double = 0.8,
  requiredObjectives: Set[ObjectiveId] = Set.empty,
  maxViolations: Int = 0,
  minCompletionTime: Option[Duration] = None,
  maxCompletionTime: Option[Duration] = None
)
