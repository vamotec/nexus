package app.mosia.nexus
package domain.model.task

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 成功标准 */
case class SuccessCriteria(
  minScore: Double = 0.8,
  requiredObjectives: Set[ObjectiveId] = Set.empty,
  maxViolations: Int = 0,
  minCompletionTime: Option[Duration] = None,
  maxCompletionTime: Option[Duration] = None
)
