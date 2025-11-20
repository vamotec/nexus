package app.mosia.nexus
package application.util

import domain.model.user.UserId
import domain.services.infra.JwtContent
import java.util.UUID
import zio.json.*
import zio.http.*
import zio.*

object EnvContext:
  // 从 JwtContent 环境中提取用户ID
  def extractUserId: ZIO[JwtContent, Response, UserId] =
    for {
      jwtContent <- ZIO.service[JwtContent]
      payload <- jwtContent.get
        .someOrFail(Response.unauthorized("JWT content not found"))
    } yield UserId(UUID.fromString(payload.userIdStr))
