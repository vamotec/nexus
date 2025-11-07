package app.mosia.nexus.infra.persistence.postgres.entity

import java.time.Instant
import java.util.UUID

case class AuditLogEntity(
  id: UUID,
  userId: Option[UUID],
  action: String,
  resourceType: Option[String],
  resourceId: Option[String],
  details: Option[String],
  ipAddress: Option[String],
  userAgent: Option[String],
  platform: Option[String],
  success: Boolean,
  errorMessage: Option[String],
  createdAt: Instant
)
