package app.mosia.nexus.domain.model.audit

import app.mosia.nexus.domain.model.user.UserId

import java.time.Instant

case class AuditLog(
  id: AuditLogId,
  userId: Option[UserId], // 可能是匿名操作（如登录失败）
  action: AuditAction,
  resourceType: Option[String], // 资源类型（如 "project", "simulation"）
  resourceId: Option[String], // 资源 ID
  details: Option[String], // JSON 格式的详细信息
  ipAddress: Option[String],
  userAgent: Option[String],
  platform: Option[String], // ios, android, web
  success: Boolean, // 操作是否成功
  errorMessage: Option[String], // 如果失败，错误信息
  createdAt: Instant
)
