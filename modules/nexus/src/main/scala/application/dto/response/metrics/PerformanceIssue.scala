package app.mosia.nexus
package application.dto.response.metrics

import java.time.Instant

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PerformanceIssue(
  severity: IssueSeverity,
  category: IssueCategory,
  description: String,
  timestamp: Instant,
  recommendation: Option[String] = None
) derives Cs.SemiAuto,
      ArgBuilder
