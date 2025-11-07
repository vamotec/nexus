package app.mosia.nexus.application.dto.response.auth

import sttp.tapir.Schema
import zio.json.JsonCodec

case class MobileRefreshResponse(
  accessToken: String,
  refreshToken: String, // 返回新的 refresh token（refresh token rotation）
  expiresIn: Long,
  refreshExpiresIn: Long
) derives JsonCodec,
      Schema
