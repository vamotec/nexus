package app.mosia.nexus
package application.dto.response.metrics

case class PerformanceMetrics(
  currentFps: Double,
  frameCount: Long,
  simulationTime: Double,
  wallTime: Double,
  timeRatio: Double // simulationTime / wallTime
)
