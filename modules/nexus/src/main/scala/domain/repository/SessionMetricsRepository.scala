package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.metrics.{AggregatedMetrics, AggregationInterval, SimSessionMetrics}

import java.time.Instant
import java.util.UUID

trait SessionMetricsRepository:
  def updateSnapshot(metrics: SimSessionMetrics): AppTask[Unit]

  def recordHistory(metrics: SimSessionMetrics): AppTask[Unit]

  def getLatest(sessionId: UUID): AppTask[Option[SimSessionMetrics]]

  def getHistory(
    sessionId: UUID,
    from: Instant,
    to: Instant
  ): AppTask[List[SimSessionMetrics]]

  def getAggregated(
    sessionId: UUID,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]]

  def getMultiSessionAggregated(
    simulationId: UUID,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]]
