package app.mosia.nexus
package domain.services.app

import application.dto.response.metrics.{PerformanceChartResponse, PerformanceReportResponse, RealtimeMetricsResponse}
import domain.error.AppTask
import domain.model.common.AbsoluteTimeRange
import domain.model.metrics.{AggregationInterval, PerformanceReport, SimSessionMetrics}
import domain.model.session.SessionId

trait MetricsService:
  def getPerformanceReport(sessionId: SessionId, duration: Long): AppTask[PerformanceReportResponse]
  def getRealtimeMetrics(sessionId: SessionId): AppTask[RealtimeMetricsResponse]
  def getPerformanceChart(
    sessionId: SessionId,
    timeRange: AbsoluteTimeRange,
    interval: String
  ): AppTask[PerformanceChartResponse]
  def recordMetrics(metrics: SimSessionMetrics): AppTask[Unit]
  def syncSessionMetrics(sessionId: SessionId, clusterId: String): AppTask[Unit]
  def calculateAvg(values: List[Double]): Double
