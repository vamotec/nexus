package app.mosia.nexus
package domain.model.user

import java.time.Instant
import java.util.UUID

case class Authenticator(
  id: UUID,
  userId: UserId,
  deviceId: String,
  keyId: String,
  publicKey: Array[Byte], // DER-encoded public key bytes
  signCount: Option[Long],
  lastUsedAt: Option[Instant]
)
