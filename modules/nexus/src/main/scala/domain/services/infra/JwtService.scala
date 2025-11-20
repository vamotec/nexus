package app.mosia.nexus
package domain.services.infra

import domain.model.project.ProjectId
import domain.error.AppTask
import domain.model.session.SessionId
import domain.model.user.UserId

import pdi.jwt.JwtClaim

trait JwtService:
  def generateToken(userId: UserId, platform: Option[String] = None): AppTask[String]

  def decode(token: String): AppTask[JwtClaim]

  def validateToken(token: String): AppTask[UserId]

  def generateSessionToken(sessionId: SessionId, userId: UserId, permissions: Set[String]): AppTask[String]

  def generateOmniverseToken(projectId: ProjectId, userId: UserId, permissions: Set[String]): AppTask[String]
