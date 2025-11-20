package app.mosia.nexus
package domain.model.metrics

import domain.model.session.SessionId
import domain.model.simulation.SimulationId

import java.time.Instant

case class AggregatedMetrics(
  bucket: Instant, // 时间桶
  sessionId: SessionId,
  simulationId: SimulationId,

  // FPS 统计
  avgFps: Double,
  maxFps: Double,
  minFps: Double,
  p50Fps: Option[Double] = None, // 中位数
  p99Fps: Option[Double] = None, // 99 分位数

  // GPU 统计
  avgGpuUtilization: Double,
  maxGpuUtilization: Option[Double] = None,
  maxGpuMemoryMb: Long,

  // 帧数统计
  totalFrames: Option[Long] = None,

  // 数据点数量
  sampleCount: Option[Long] = None
)
