package app.mosia.nexus
package domain.config.auth

import domain.config.auth.ExpirationConfig

case class TokenConfig(
  secret: String,
  issuer: String,
  expiration: ExpirationConfig,
  algorithm: String
)
