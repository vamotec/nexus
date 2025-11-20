package app.mosia.nexus
package application.dto.response.metrics

import application.dto.response.common.*

import java.time.Instant

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class PerformanceDataPoint(
  timestamp: Instant,
  // FPS 统计
  fps: FpsStats,
  // GPU 统计
  gpu: GpuStats,
  // 帧统计
  frames: Option[FrameStats] = None
) derives Cs.SemiAuto,
      ArgBuilder
