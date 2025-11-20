package app.mosia.nexus
package presentation.graphql.schema

import application.dto.response.metrics.*
import domain.error.CalTask
import domain.model.common.StringTimeRange
import domain.model.metrics.{AggregatedMetrics, SimSessionMetrics}
import domain.model.session.SessionId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

object MetricsSchema:

  case class performanceChartArgs(
    sessionId: String,
    timeRange: StringTimeRange,
    interval: String
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class PerformanceReportArgs(
    sessionId: String,
    durationSeconds: Long
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class MetricsQueries(
    realtimeMetrics: String => CalTask[RealtimeMetricsResponse],
    performanceChart: performanceChartArgs => CalTask[PerformanceChartResponse],
    performancereport: PerformanceReportArgs => CalTask[PerformanceReportResponse]
  ) derives Cs.SemiAuto
