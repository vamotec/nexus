package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.json.*

@jsonMemberNames(SnakeCase)
case class RegisterResponse(
  userId: String,
  email: String,
  emailVerified: Boolean,
  message: String,
  verificationToken: Option[String] = None  // 临时验证令牌，用于访问验证页面
) derives JsonCodec,
      Schema
