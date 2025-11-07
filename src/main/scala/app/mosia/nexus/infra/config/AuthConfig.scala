package app.mosia.nexus.infra.config

case class AuthConfig(
  token: TokenConfig,
  password: PasswordConfig,
  oauth2: OAuth2Config,
  baseUrl: String
)
