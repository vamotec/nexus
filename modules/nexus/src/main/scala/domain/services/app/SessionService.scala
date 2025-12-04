package app.mosia.nexus
package domain.services.app

import application.dto.request.session.CreateSessionRequest
import application.dto.response.session.SessionResponse
import domain.error.AppTask
import domain.model.project.ProjectId
import domain.model.session.SessionId
import domain.model.simulation.SimulationId
import domain.model.user.UserId

trait SessionService:
  def createSession(
    userId: UserId,
    simulationId: SimulationId,
    request: CreateSessionRequest,
    userIp: String
  ): AppTask[SessionResponse]
  def startSession(sessionId: SessionId): AppTask[SessionResponse]
  def stopSession(sessionId: SessionId, reason: String = "User requested"): AppTask[SessionResponse]
  def getSessionById(sessionId: SessionId): AppTask[Option[SessionResponse]]
  def listUserSessions(userId: UserId, projectId: ProjectId, limit: Int = 100): AppTask[List[SessionResponse]]
  def generateControlToken(userId: String, sessionId: String): AppTask[String]
