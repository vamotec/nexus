package app.mosia.nexus
package application.dto.response.metrics

import domain.model.common.AbsoluteTimeRange
import domain.model.metrics.AggregatedMetrics

case class AggregatedMetricsResponse(
  timeRange: AbsoluteTimeRange,
  interval: String,
  data: List[AggregatedMetrics]
)
