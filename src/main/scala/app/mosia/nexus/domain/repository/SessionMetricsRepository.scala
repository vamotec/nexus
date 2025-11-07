package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.session.SessionMetrics
import app.mosia.nexus.infra.error.AppTask

import java.time.Instant
import java.util.UUID

trait SessionMetricsRepository:
  def insert(metric: SessionMetrics): AppTask[Unit]

  def findLatest(sessionId: UUID): AppTask[Option[SessionMetrics]]

  def getHistory(
    sessionId: UUID,
    from: Instant,
    to: Instant,
    limit: Int
  ): AppTask[List[SessionMetrics]]
