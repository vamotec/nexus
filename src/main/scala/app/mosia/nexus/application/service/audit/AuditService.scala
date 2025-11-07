package app.mosia.nexus.application.service.audit

import app.mosia.nexus.domain.model.audit.{AuditAction, AuditLog, AuditLogFilters}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

trait AuditService:
  /** 记录登录成功
    */
  def logLogin(
    userId: UserId,
    platform: Option[String] = None,
    ipAddress: Option[String] = None,
    userAgent: Option[String] = None
  ): AppTask[Unit]

  /** 记录登录失败
    */
  def logLoginFailed(
    username: String,
    reason: String,
    ipAddress: Option[String] = None,
    userAgent: Option[String] = None
  ): AppTask[Unit]

  /** 记录登出
    */
  def logLogout(
    userId: UserId,
    platform: Option[String] = None
  ): AppTask[Unit]

  /** 记录密码修改
    */
  def logPasswordChanged(userId: UserId): AppTask[Unit]

  /** 记录密码重置请求
    */
  def logPasswordResetRequested(userId: UserId): AppTask[Unit]

  /** 记录密码重置完成
    */
  def logPasswordReset(userId: UserId): AppTask[Unit]

  /** 记录资源创建
    */
  def logResourceCreated(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String,
    details: Option[String] = None
  ): AppTask[Unit]

  /** 记录资源更新
    */
  def logResourceUpdated(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String,
    details: Option[String] = None
  ): AppTask[Unit]

  /** 记录资源删除
    */
  def logResourceDeleted(
    userId: UserId,
    action: AuditAction,
    resourceType: String,
    resourceId: String
  ): AppTask[Unit]

  /** 记录通用操作
    */
  def logAction(
    userId: Option[UserId],
    action: AuditAction,
    resourceType: Option[String] = None,
    resourceId: Option[String] = None,
    details: Option[String] = None,
    success: Boolean = true,
    errorMessage: Option[String] = None,
    ipAddress: Option[String] = None,
    userAgent: Option[String] = None,
    platform: Option[String] = None
  ): AppTask[Unit]

  /** 获取用户的审计日志
    */
  def getUserAuditLogs(userId: UserId, limit: Int = 100): AppTask[List[AuditLog]]

  /** 根据过滤器查询审计日志
    */
  def queryAuditLogs(filters: AuditLogFilters, limit: Int = 100): AppTask[List[AuditLog]]

  /** 获取最近的审计日志
    */
  def getRecentAuditLogs(limit: Int = 100): AppTask[List[AuditLog]]

  /** 清理旧的审计日志（定时任务）
    */
  def cleanupOldLogs(olderThanDays: Int = 90): AppTask[Long]
