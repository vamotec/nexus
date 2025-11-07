package app.mosia.nexus.domain.model.task

import zio.Duration

/** 失败标准 */
case class FailureCriteria(
  maxCollisions: Int = 1,
  maxTrafficViolations: Int = 3,
  maxTimeExceeded: Option[Duration] = None,
  minProgress: Double = 0.0,
  catastrophicEvents: Set[String] = Set("flip", "explosion")
)
