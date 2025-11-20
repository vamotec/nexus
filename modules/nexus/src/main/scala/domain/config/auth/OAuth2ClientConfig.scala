package app.mosia.nexus
package domain.config.auth

case class OAuth2ClientConfig(
  clientId: String,
  clientSecret: String,
  authorizationUrl: String,
  tokenUrl: String,
  userInfoUrl: String,
  scope: String
)
