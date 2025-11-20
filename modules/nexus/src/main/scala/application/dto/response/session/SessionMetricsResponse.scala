package app.mosia.nexus
package application.dto.response.session

import domain.model.common.Position3D

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 会话指标响应 DTO
  *
  * 用于返回会话运行时的实时指标信息
  */
case class SessionMetricsResponse(
  // 性能指标
  fps: Double,
  frameCount: Long,

  // 时间指标
  simulationTime: Double, // 仿真时间（秒）
  wallTime: Double, // 实际运行时间（秒）
  timeRatio: Double, // 时间比例（仿真时间/实际时间）

  // 资源使用
  gpuUtilization: Double, // GPU 使用率 (0-100)
  gpuMemoryMB: Double, // GPU 内存使用 (MB)

  // 机器人状态（可选）
  robotPosition: Option[Position3D],

  // 最后更新时间
  lastUpdatedAt: Long // Unix timestamp (毫秒)
) derives Cs.SemiAuto,
      ArgBuilder
