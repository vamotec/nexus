package app.mosia.nexus
package application.dto.response.metrics

import domain.model.common.AbsoluteTimeRange

case class PerformanceChartResponse(
  sessionId: String,
  timeRange: AbsoluteTimeRange,
  interval: String, // "1min", "1hour", "1day"
  dataPoints: List[PerformanceDataPoint]
)
