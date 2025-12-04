package app.mosia.nexus
package application.dto.response.metrics

import domain.model.common.AbsoluteTimeRange

case class PerformanceReportResponse(
  sessionId: String,
  simulationId: String,
  timeRange: AbsoluteTimeRange,

  // 概览统计

  summary: PerformanceSummary,

  // 图表数据

  chart: PerformanceChartResponse,

  // 问题检测

  issues: List[PerformanceIssue]
)
