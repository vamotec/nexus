package app.mosia.nexus
package domain.model.user

case class TokenPair(
  accessToken: String, // JWT
  refreshToken: String, // 随机字符串或JWT
  expiresIn: Long
)
