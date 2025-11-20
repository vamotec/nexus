package app.mosia.nexus
package application.dto.response.metrics

import domain.model.common.AbsoluteTimeRange

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PerformanceChartResponse(
  sessionId: String,
  timeRange: AbsoluteTimeRange,
  interval: String, // "1min", "1hour", "1day"
  dataPoints: List[PerformanceDataPoint]
) derives Cs.SemiAuto,
      ArgBuilder
