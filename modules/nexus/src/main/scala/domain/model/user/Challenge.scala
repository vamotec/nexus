package app.mosia.nexus
package domain.model.user

import java.time.Instant
import java.util.UUID

case class Challenge(
  id: UUID,
  userId: Option[UserId],
  deviceId: Option[String],
  challenge: String,
  purpose: String,
  expiresAt: Instant,
  consumed: Option[Boolean]
)
