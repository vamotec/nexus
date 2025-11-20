package app.mosia.nexus
package domain.model.audit

import domain.error.AppTask
import domain.model.common.EntityId

import zio.json.*

import java.util.UUID

case class AuditLogId(value: UUID) extends EntityId[AuditLogId] derives JsonCodec

object AuditLogId:
  /** 从字符串解析 DeviceId */
  def fromString(str: String): AppTask[AuditLogId] =
    EntityId.fromString(str)(using AuditLogId.apply)

  /** ✅ 生成一个新的随机 DeviceId */
  def generate(): AuditLogId =
    AuditLogId(UUID.randomUUID())
