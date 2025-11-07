package app.mosia.nexus.infra.persistence.postgres.entity

import java.time.Instant
import java.util.UUID

case class RefreshTokenEntity(
  token: String,
  userId: UUID,
  expiresAt: Instant,
  deviceInfo: Option[String] = None,
  isRevoked: Boolean = false
)
