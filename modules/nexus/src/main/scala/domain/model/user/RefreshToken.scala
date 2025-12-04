package app.mosia.nexus
package domain.model.user

import java.time.Instant
import java.util.UUID

case class RefreshToken(
  id: Long,
  token: String,
  userId: String,
  expiresAt: Instant,
  isRevoked: Option[Boolean]
)
