package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.audit.{AuditLog, AuditLogFilters, AuditLogId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

import java.time.Instant

trait AuditLogRepository:
  def insert(log: AuditLog): AppTask[AuditLog]

  def findById(id: AuditLogId): AppTask[Option[AuditLog]]

  def findByUserId(userId: UserId, limit: Int = 100): AppTask[List[AuditLog]]

  def findByFilters(filters: AuditLogFilters, limit: Int = 100): AppTask[List[AuditLog]]

  def findRecent(limit: Int = 100): AppTask[List[AuditLog]]

  def countByUserId(userId: UserId): AppTask[Long]

  def deleteOlderThan(timestamp: Instant): AppTask[Long]
