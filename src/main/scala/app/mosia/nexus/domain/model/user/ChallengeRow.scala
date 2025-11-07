package app.mosia.nexus.domain.model.user

import java.time.Instant
import java.util.UUID

case class ChallengeRow(
  id: UUID,
  userId: Option[UserId],
  deviceId: Option[String],
  challenge: String,
  purpose: String,
  expiresAt: Instant,
  consumed: Boolean
)
