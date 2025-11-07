package app.mosia.nexus.application.util

import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.auth.JwtContent
import zio.ZIO
import zio.http.Response

import java.util.UUID

object EnvContext:
  // 从 JwtContent 环境中提取用户ID
  def extractUserId: ZIO[JwtContent, Response, UserId] =
    for {
      jwtContent <- ZIO.service[JwtContent]
      payload <- jwtContent.get
        .someOrFail(Response.unauthorized("JWT content not found"))
    } yield UserId(UUID.fromString(payload.userIdStr))
