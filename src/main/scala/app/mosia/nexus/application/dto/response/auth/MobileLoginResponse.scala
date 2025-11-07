package app.mosia.nexus.application.dto.response.auth

import app.mosia.nexus.application.dto.response.user.UserResponse
import sttp.tapir.Schema
import zio.json.JsonCodec

case class MobileLoginResponse(
  user: UserResponse,
  accessToken: String, // 添加 access token
  refreshToken: String, // 添加 refresh token
  expiresIn: Long, // access token 过期时间（秒）
  refreshExpiresIn: Long // refresh token 过期时间（秒）
) derives JsonCodec,
      Schema
