package app.mosia.nexus.infra.config

import zio.Duration

case class ExpirationConfig(
  accessToken: Duration,
  refreshToken: Duration,
  controlToken: Duration
)
