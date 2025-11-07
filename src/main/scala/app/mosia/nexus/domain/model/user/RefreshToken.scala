package app.mosia.nexus.domain.model.user

import java.time.Instant
import java.util.UUID

case class RefreshToken(
  id: UUID,
  token: String,
  userId: UserId,
  expiresAt: Instant,
  isRevoked: Boolean = false
)
