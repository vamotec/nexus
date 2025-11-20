package app.mosia.nexus
package domain.config.auth

/** 认证配置 */
case class AuthConfig(
  token: TokenConfig,
  oauth: OAuth2Config,
  baseUrl: String
)
