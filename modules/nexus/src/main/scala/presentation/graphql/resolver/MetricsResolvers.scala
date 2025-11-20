package app.mosia.nexus
package presentation.graphql.resolver

import domain.services.app.MetricsService
import presentation.graphql.schema.MetricsSchema.MetricsQueries
import application.dto.response.metrics.{PerformanceMetrics, RealtimeMetricsResponse}
import application.util.TimeRangeConverter
import domain.error.*
import domain.model.common.Position3D
import domain.model.metrics.SimSessionMetrics
import domain.model.session.SessionId

import java.time.Instant
import java.util.UUID

object MetricsResolvers:
  def queries(service: MetricsService) = MetricsQueries(
    realtimeMetrics = sessionId =>
      (for
        id <- SessionId.fromString(sessionId)
        response <- service.getRealtimeMetrics(id)
      yield response).mapError(toCalibanError),
    performanceChart = args =>
      (for
        id <- SessionId.fromString(args.sessionId)
        timeRange <- TimeRangeConverter.parse(args.timeRange).mapError(msg => InvalidInput("TimeRange", msg))
        response <- service.getPerformanceChart(id, timeRange, args.interval)
      yield response).mapError(toCalibanError),
    performancereport = args =>
      (for
        id <- SessionId.fromString(args.sessionId)
        response <- service.getPerformanceReport(id, args.durationSeconds)
      yield response).mapError(toCalibanError)
  )
