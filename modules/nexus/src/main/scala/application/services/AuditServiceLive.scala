package app.mosia.nexus
package application.services

import domain.error.AppTask
import domain.model.audit.{AuditAction, AuditLog, AuditLogFilters, AuditLogId}
import domain.model.user.UserId
import domain.repository.AuditLogRepository
import domain.services.app.AuditService

import app.mosia.nexus.application.util.QueryParser
import app.mosia.nexus.domain.error.ValidationError.InvalidInput
import zio.json.*
import zio.*
import zio.json.ast.Json

import java.time.Instant
import java.time.temporal.ChronoUnit

class AuditServiceLive(
  auditLogRepository: AuditLogRepository
) extends AuditService:

  override def logLogin(
    userId: UserId,
    platform: Option[String],
    ipAddress: Option[String],
    userAgent: Option[String]
  ): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = AuditAction.Login,
      platform = platform,
      ipAddress = ipAddress,
      userAgent = userAgent
    )

  override def logLoginFailed(
    username: String,
    reason: String,
    ipAddress: Option[String],
    userAgent: Option[String]
  ): AppTask[Unit] =
    val details = Map(
      "username" -> username,
      "reason" -> reason
    ).toJson

    logAction(
      userId = None, // 登录失败时可能没有 userId
      action = AuditAction.LoginFailed,
      details = Some(details),
      success = false,
      errorMessage = Some(reason),
      ipAddress = ipAddress,
      userAgent = userAgent
    )

  override def logLogout(
    userId: UserId,
    platform: Option[String]
  ): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = AuditAction.Logout,
      platform = platform
    )

  override def logPasswordChanged(userId: UserId): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = AuditAction.PasswordChanged
    )

  override def logPasswordResetRequested(userId: UserId): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = AuditAction.PasswordResetRequested
    )

  override def logPasswordReset(userId: UserId): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = AuditAction.PasswordReset
    )

  override def logResourceCreated(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String,
    details: Option[String]
  ): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = action,
      resourceType = Some(resourceType),
      resourceId = Some(resourceId),
      details = details
    )

  override def logResourceUpdated(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String,
    details: Option[String]
  ): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = action,
      resourceType = Some(resourceType),
      resourceId = Some(resourceId),
      details = details
    )

  override def logResourceDeleted(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String
  ): AppTask[Unit] =
    logAction(
      userId = Some(userId),
      action = action,
      resourceType = Some(resourceType),
      resourceId = Some(resourceId)
    )

  override def logAction(
    userId: Option[UserId],
    action: AuditAction,
    resourceType: Option[String],
    resourceId: Option[String],
    details: Option[String],
    success: Boolean,
    errorMessage: Option[String],
    ipAddress: Option[String],
    userAgent: Option[String],
    platform: Option[String]
  ): AppTask[Unit] =
    val detailsJson: Either[String, Option[Json]] = QueryParser.parseOptionalJson(details)
    val addressJson: Either[String, Option[Json]] = QueryParser.parseOptionalJson(ipAddress)
    val log                                       = AuditLog(
      id = AuditLogId.generate(),
      userId = userId,
      action = action,
      resourceType = resourceType,
      resourceId = resourceId,
      details = detailsJson.getOrElse(throw InvalidInput("json", "parse failed")),
      ipAddress = addressJson.getOrElse(throw InvalidInput("json", "parse failed")),
      userAgent = userAgent,
      platform = platform,
      success = success,
      errorMessage = errorMessage,
      createdAt = Instant.now()
    )

    auditLogRepository
      .insert(log)
      .unit
      .catchAll { error =>
        // 审计日志失败不应该影响主流程
        ZIO.logWarning(s"Failed to insert audit log: ${error.getMessage}")
      }

  override def getUserAuditLogs(userId: UserId, limit: Int): AppTask[List[AuditLog]] =
    auditLogRepository.findByUserId(userId, limit)

  override def queryAuditLogs(filters: AuditLogFilters, limit: Int): AppTask[List[AuditLog]] =
    auditLogRepository.findByFilters(filters, limit)

  override def getRecentAuditLogs(limit: Int): AppTask[List[AuditLog]] =
    auditLogRepository.findRecent(limit)

  override def cleanupOldLogs(olderThanDays: Int): AppTask[Long] =
    val cutoffDate = Instant.now().minus(olderThanDays.toLong, ChronoUnit.DAYS)
    auditLogRepository.deleteOlderThan(cutoffDate)

object AuditServiceLive:
  val live: ZLayer[AuditLogRepository, Nothing, AuditService] =
    ZLayer.fromFunction(new AuditServiceLive(_))
