package app.mosia.nexus.application.service.session

import app.mosia.nexus.application.dto.request.session.CreateSessionRequest
import app.mosia.nexus.application.dto.response.session.SessionResponse
import zio.*
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId

trait SessionService:
  def createSession(userId: UserId, simulationId: SimulationId, request: CreateSessionRequest): Task[SessionResponse]
  def startSession(sessionId: SessionId): Task[SessionResponse]
  def stopSession(sessionId: SessionId, reason: String = "User requested"): Task[SessionResponse]
  def getSession(sessionId: SessionId): Task[Option[SessionResponse]]
  def listUserSessions(userId: UserId, projectId: ProjectId, limit: Int = 100): Task[List[SessionResponse]]
  def syncSessionMetrics(sessionId: SessionId): Task[Unit]
