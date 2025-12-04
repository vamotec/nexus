package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.model.jwt.Payload
import domain.model.project.ProjectId
import domain.model.session.SessionId
import domain.model.user.UserId

import pdi.jwt.JwtClaim

trait JwtService:
  def generateToken(sub: String, payload: Payload, audience: String, platform: Option[String] = None): AppTask[String]

  def decode(token: String): AppTask[JwtClaim]

  def validateToken(token: String): AppTask[String]
