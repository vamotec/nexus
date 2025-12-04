package app.mosia.nexus
package application.dto.response.metrics

import domain.model.common.{Position3D, Velocity}

import java.time.Instant

case class RealtimeMetricsResponse(
  sessionId: String,
  simulationId: String,
  timestamp: Instant,

  // 性能指标
  performance: PerformanceMetrics,

  // 机器人状态
  position: Position3D,
  velocity: Option[Velocity] = None,

  // 资源使用
  utilization: Double, // 0-100
  memoryUsed: Long, // MB
  memoryTotal: Option[Long] = None
)
