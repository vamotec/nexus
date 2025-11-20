package app.mosia.nexus
package application.services

import application.dto.response.common.{FpsStats, FrameStats, GpuStats}
import application.dto.response.metrics.*
import application.util.toZIOOrFail
import domain.error.*
import domain.model.common.AbsoluteTimeRange
import domain.model.metrics.*
import domain.model.session.{SessionId, SessionStatus, SimSession}
import domain.repository.{SessionMetricsRepository, SessionRepository}
import domain.services.app.MetricsService
import domain.services.infra.NeuroClient

import zio.json.*
import zio.*
import zio.json.ast.Json

import java.time.Instant

class MetricsServiceLive(
  sessionRepo: SessionRepository,
  metricsRepo: SessionMetricsRepository,
  neuroClient: NeuroClient
) extends MetricsService:

  override def getPerformanceReport(
    sessionId: SessionId,
    duration: Long
  ): AppTask[PerformanceReportResponse] =
    val now  = Instant.now()
    val from = now.minusSeconds(duration)

    for
      // 并行获取数据
      (session, aggregated) <- sessionRepo
        .findById(sessionId)
        .someOrFail(NotFound("Session", sessionId.value.toString))
        .zipPar(
          metricsRepo.getAggregated(sessionId.value, from, now, AggregationInterval.Hour)
        )
      // 计算概览统计
      summary = calculateSummary(aggregated, duration)
      // 生成图表数据
      chart = PerformanceChartResponse(
        sessionId = sessionId.toString,
        timeRange = AbsoluteTimeRange(from, now),
        interval = "1hour",
        dataPoints = aggregated.map(toDataPoint)
      )
      // 检测性能问题
      issues = detectIssues(aggregated)

      report = PerformanceReportResponse(
        sessionId = sessionId.toString,
        simulationId = session.simulationId.toString,
        timeRange = AbsoluteTimeRange(from, now),
        summary = summary,
        chart = chart,
        issues = issues
      )
    yield report

  /** 更新会话指标（由后台任务定期调用） */
  override def syncSessionMetrics(sessionId: SessionId, clusterId: String): AppTask[Unit] =
    (for
      // 1. 获取会话
      sessionOpt <- sessionRepo.findById(sessionId)
      session <- ZIO
        .fromOption(sessionOpt)
        .orElseFail(new NoSuchElementException(s"Session not found: ${sessionId.value}"))

      // 2. 只同步运行中的会话
      _ <- ZIO.when(session.status == SessionStatus.Running)(
        for
          // 3. 调用 Neuro gRPC 获取状态
          grpcRequest <- ZIO.succeed(
            domain.grpc.neuro.GetSessionStatusRequest(
              sessionId = sessionId.value.toString
            )
          )

          grpcResponse <- neuroClient.getSessionStatus(clusterId, grpcRequest)

          // 4. 解析 gRPC 响应并转换为领域模型
          metrics <- grpcResponse.sessionStatus match
            case Some(grpcStatus) =>
              ZIO.succeed(
                SimSessionMetrics(
                  sessionId = sessionId,
                  simulationId = session.simulationId,
                  currentFps = grpcStatus.fps,
                  frameCount = grpcStatus.frameCount,
                  simulationTime = grpcStatus.simulationTime,
                  wallTime =
                    (Instant.now().toEpochMilli - session.startedAt.getOrElse(session.createdAt).toEpochMilli) / 1000.0,
                  robotPosition = domain.model.common.Position3D(0.0, 0.0, 0.0), // TODO: 从 grpcStatus 提取
                  gpuUtilization = grpcStatus.gpuUtilization.toDouble,
                  gpuMemoryMB = grpcStatus.gpuMemoryMb.toDouble,
                  updatedAt = Instant.now(),
                  tags = Some(
                    Json.Obj(
                      "status" -> Json.Str(grpcStatus.status.toString),
                      "cpu_utilization" -> Json.Num(grpcStatus.cpuUtilization)
                    )
                  )
                )
              )
            case None =>
              ZIO.fail(new RuntimeException("No status in gRPC response"))

          // 5. 写入 TimescaleDB
          _ <- metricsRepo.updateSnapshot(metrics)

          _ <- ZIO.logInfo(
            s"Synced metrics for session ${sessionId.value}: fps=${metrics.currentFps}, frames=${metrics.frameCount}"
          )
        yield ()
      )
    yield ()).mapError(toAppError)

  override def calculateAvg(values: List[Double]): Double =
    if values.isEmpty then 0.0
    else values.sum / values.length

  override def getRealtimeMetrics(sessionId: SessionId): AppTask[RealtimeMetricsResponse] =
    for
      metricsOpt <- metricsRepo.getLatest(sessionId.value)
      metrics <- metricsOpt.toZIOOrFail(NotFound("Metrics", sessionId.value.toString))
      sessionOpt <- sessionRepo.findById(sessionId)
      session <- sessionOpt.toZIOOrFail(NotFound("Session", sessionId.value.toString))
    yield toRealtimeResponse(metrics, session)

  override def recordMetrics(metrics: SimSessionMetrics): AppTask[Unit] =
    metricsRepo.recordHistory(metrics)

  override def getPerformanceChart(
    sessionId: SessionId,
    timeRange: AbsoluteTimeRange,
    interval: String
  ): AppTask[PerformanceChartResponse] =
    for
      aggregated <- metricsRepo.getAggregated(
        sessionId.value,
        timeRange.start,
        timeRange.end,
        AggregationInterval.fromString(interval)
      )

      chart = PerformanceChartResponse(
        sessionId = sessionId.value.toString,
        timeRange = timeRange,
        interval = interval,
        dataPoints = aggregated.map(toDataPoint)
      )
    yield chart

  /// ============================================================================ ///
  /// 辅助函数
  /// ============================================================================ ///

  private def toRealtimeResponse(
    metrics: SimSessionMetrics,
    session: SimSession
  ): RealtimeMetricsResponse =
    RealtimeMetricsResponse(
      sessionId = metrics.sessionId.toString,
      simulationId = metrics.simulationId.toString,
      timestamp = Instant.now(),
      performance = PerformanceMetrics(
        currentFps = metrics.currentFps,
        frameCount = metrics.frameCount,
        simulationTime = metrics.simulationTime,
        wallTime = metrics.wallTime,
        timeRatio =
          if metrics.wallTime > 0
          then metrics.simulationTime / metrics.wallTime
          else 0.0
      ),
      position = metrics.robotPosition,
      utilization = metrics.gpuUtilization,
      memoryUsed = metrics.gpuMemoryMB.toLong
    )

  private def toDataPoint(m: AggregatedMetrics): PerformanceDataPoint =
    PerformanceDataPoint(
      timestamp = m.bucket,
      fps = FpsStats(
        avg = m.avgFps,
        max = m.maxFps,
        min = m.minFps,
        p50 = m.p50Fps,
        p99 = m.p99Fps
      ),
      gpu = GpuStats(
        avgUtilization = m.avgGpuUtilization,
        maxUtilization = m.maxGpuUtilization,
        maxMemoryMb = m.maxGpuMemoryMb
      ),
      frames = m.totalFrames.map(total =>
        FrameStats(
          total = total,
          rate = total / 3600.0 // 假设是小时数据
        )
      )
    )

  private def calculateSummary(
    metrics: List[AggregatedMetrics],
    duration: Long
  ): PerformanceSummary =
    val avgFps      = metrics.map(_.avgFps).sum / metrics.length
    val maxFps      = metrics.map(_.maxFps).maxOption.getOrElse(0.0)
    val minFps      = metrics.map(_.minFps).minOption.getOrElse(0.0)
    val p99Fps      = metrics.flatMap(_.p99Fps).lastOption
    val totalFrames = metrics.flatMap(_.totalFrames).sum
    val avgGpu      = metrics.map(_.avgGpuUtilization).sum / metrics.length
    val peakGpuMem  = metrics.map(_.maxGpuMemoryMb).maxOption.getOrElse(0L)

    PerformanceSummary(
      avgFps = avgFps,
      maxFps = maxFps,
      minFps = minFps,
      p99Fps = p99Fps,
      totalFrames = totalFrames,
      avgGpuUtilization = avgGpu,
      peakGpuMemoryMb = peakGpuMem,
      duration = duration,
      health = determineHealth(avgFps)
    )

  private def determineHealth(avgFps: Double): HealthStatus =
    if avgFps > 55 then HealthStatus.Excellent
    else if avgFps > 45 then HealthStatus.Good
    else if avgFps > 30 then HealthStatus.Fair
    else HealthStatus.Poor

  private def detectIssues(
    metrics: List[AggregatedMetrics]
  ): List[PerformanceIssue] =
    val issues = scala.collection.mutable.ListBuffer[PerformanceIssue]()
    // 检测低 FPS
    metrics.foreach { m =>
      if m.avgFps < 30 then
        issues += PerformanceIssue(
          severity = IssueSeverity.Critical,
          category = IssueCategory.LowFps,
          description = f"FPS dropped to ${m.avgFps}%.1f at ${m.bucket}",
          timestamp = m.bucket,
          recommendation = Some("Consider reducing scene complexity or upgrading GPU")
        )
    }
    // 检测高 GPU 使用率
    metrics.foreach { m =>
      if m.avgGpuUtilization > 90 then
        issues += PerformanceIssue(
          severity = IssueSeverity.Warning,
          category = IssueCategory.HighGpuUsage,
          description = f"GPU utilization reached ${m.avgGpuUtilization}%.1f%% at ${m.bucket}",
          timestamp = m.bucket,
          recommendation = Some("GPU is near maximum capacity")
        )
    }
    // 检测 FPS 不稳定
    val fpsVariance = calculateVariance(metrics.map(_.avgFps))
    if fpsVariance > 100 then // 标准差 > 10
      issues += PerformanceIssue(
        severity = IssueSeverity.Warning,
        category = IssueCategory.Instability,
        description = "FPS is unstable with high variance",
        timestamp = Instant.now(),
        recommendation = Some("Check for background processes or thermal throttling")
      )

    issues.toList

  private def calculateVariance(values: List[Double]): Double =
    if values.isEmpty then 0.0
    else
      val mean = values.sum / values.length
      values.map(v => math.pow(v - mean, 2)).sum / values.length

object MetricsServiceLive:
  val live: ZLayer[SessionRepository & (SessionMetricsRepository & NeuroClient), Nothing, MetricsServiceLive] =
    ZLayer.fromFunction(new MetricsServiceLive(_, _, _))
