package app.mosia.nexus
package application.dto.response.metrics

import java.time.Instant


case class PerformanceIssue(
  severity: IssueSeverity,
  category: IssueCategory,
  description: String,
  timestamp: Instant,
  recommendation: Option[String] = None
)
