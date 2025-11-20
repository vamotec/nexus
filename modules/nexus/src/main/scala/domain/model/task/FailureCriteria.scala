package app.mosia.nexus
package domain.model.task

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 失败标准 */
case class FailureCriteria(
  maxCollisions: Int = 1,
  maxTrafficViolations: Int = 3,
  maxTimeExceeded: Option[Duration] = None,
  minProgress: Double = 0.0,
  catastrophicEvents: Set[String] = Set("flip", "explosion")
)
