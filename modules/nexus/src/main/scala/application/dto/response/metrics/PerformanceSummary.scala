package app.mosia.nexus
package application.dto.response.metrics

case class PerformanceSummary(
  avgFps: Double,
  maxFps: Double,
  minFps: Double,
  p99Fps: Option[Double],
  totalFrames: Long,
  avgGpuUtilization: Double,
  peakGpuMemoryMb: Long,
  duration: Long,
  health: HealthStatus
)
