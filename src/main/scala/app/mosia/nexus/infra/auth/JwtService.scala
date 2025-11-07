package app.mosia.nexus.infra.auth

import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask
import pdi.jwt.JwtClaim
import zio.*

import java.util.UUID

trait JwtService:
  def generateToken(userId: UserId, platform: Option[String] = None): AppTask[String]

  def decode(token: String): AppTask[JwtClaim]

  def validateToken(token: String): AppTask[UserId]
