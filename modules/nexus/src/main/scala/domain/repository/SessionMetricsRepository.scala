package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.metrics.{AggregatedMetrics, AggregationInterval, SimSessionMetrics}

import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.simulation.SimulationId

import java.time.Instant
import java.util.UUID

trait SessionMetricsRepository:
  def updateSnapshot(metrics: SimSessionMetrics): AppTask[Unit]

  def recordHistory(metrics: SimSessionMetrics): AppTask[Unit]

  def getLatest(sessionId: SessionId): AppTask[Option[SimSessionMetrics]]

  def getHistory(
    sessionId: SessionId,
    from: Instant,
    to: Instant
  ): AppTask[List[SimSessionMetrics]]

  def getAggregated(
    sessionId: SessionId,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]]

  def getMultiSessionAggregated(
    simulationId: SimulationId,
    from: Instant,
    to: Instant,
    interval: AggregationInterval
  ): AppTask[List[AggregatedMetrics]]
