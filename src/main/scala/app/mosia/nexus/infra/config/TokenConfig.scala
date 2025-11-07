package app.mosia.nexus.infra.config

case class TokenConfig(
  secret: String,
  issuer: String,
  audience: String,
  expiration: ExpirationConfig,
  algorithm: String
)
