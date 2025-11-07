package app.mosia.nexus.infra.config

case class OAuth2ClientConfig(
  clientId: String,
  clientSecret: String,
  authorizationUrl: String,
  tokenUrl: String,
  userInfoUrl: String,
  scope: String
)
