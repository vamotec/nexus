package app.mosia.nexus
package application.dto.response.metrics

import application.dto.response.common.*

import java.time.Instant

case class PerformanceDataPoint(
  timestamp: Instant,
  // FPS 统计
  fps: FpsStats,
  // GPU 统计
  gpu: GpuStats,
  // 帧统计
  frames: Option[FrameStats] = None
)
