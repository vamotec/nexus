package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.audit.{AuditLog, AuditLogFilters, AuditLogId}
import domain.model.user.UserId

import java.time.Instant

trait AuditLogRepository:
  def insert(log: AuditLog): AppTask[AuditLog]

  def findById(id: AuditLogId): AppTask[Option[AuditLog]]

  def findByUserId(userId: UserId, limit: Int = 100): AppTask[List[AuditLog]]

  def findByFilters(filters: AuditLogFilters, limit: Int = 100): AppTask[List[AuditLog]]

  def findRecent(limit: Int = 100): AppTask[List[AuditLog]]

  def countByUserId(userId: UserId): AppTask[Long]

  def deleteOlderThan(timestamp: Instant): AppTask[Long]
