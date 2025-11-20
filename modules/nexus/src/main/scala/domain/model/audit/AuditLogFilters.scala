package app.mosia.nexus
package domain.model.audit

import domain.model.user.UserId

import java.time.Instant

case class AuditLogFilters(
  userId: Option[UserId] = None,
  actions: Option[List[AuditAction]] = None,
  resourceType: Option[String] = None,
  resourceId: Option[String] = None,
  startDate: Option[Instant] = None,
  endDate: Option[Instant] = None,
  successOnly: Option[Boolean] = None,
  platform: Option[String] = None
)
