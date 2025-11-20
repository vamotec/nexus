package app.mosia.nexus
package domain.config.auth

import zio.Duration

case class ExpirationConfig(
  accessToken: Duration,
  refreshToken: Duration,
  controlToken: Duration
)
