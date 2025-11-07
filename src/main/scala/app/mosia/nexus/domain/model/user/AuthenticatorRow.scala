package app.mosia.nexus.domain.model.user

import java.time.Instant
import java.util.UUID

case class AuthenticatorRow(
  id: UUID,
  userId: UserId,
  deviceId: String,
  keyId: String,
  publicKey: Array[Byte], // DER-encoded public key bytes
  signCount: Long,
  lastUsedAt: Option[Instant]
)
