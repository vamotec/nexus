package app.mosia.nexus
package domain.model.metrics

import domain.model.common.AbsoluteTimeRange
import domain.model.session.SessionId

case class PerformanceReport(
  sessionId: SessionId,
  timeRange: AbsoluteTimeRange,
  avgFps: Double,
  maxFps: Double,
  minFps: Double,
  p99Fps: Option[Double],
  avgGpuUtilization: Double,
  dataPoints: List[AggregatedMetrics]
)
