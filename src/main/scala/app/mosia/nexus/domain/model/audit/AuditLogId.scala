package app.mosia.nexus.domain.model.audit

import app.mosia.nexus.domain.model.common.EntityId
import zio.json.JsonCodec

import java.util.UUID

case class AuditLogId(value: UUID) extends EntityId[AuditLogId] derives JsonCodec

object AuditLogId:
  /** 从字符串解析 DeviceId */
  def fromString(str: String): Either[String, AuditLogId] =
    EntityId.fromString(str)(AuditLogId.apply)

  /** ZIO 版本的 fromString */
  def fromStringZIO(str: String): zio.IO[Throwable, AuditLogId] =
    zio.ZIO.fromEither(fromString(str).left.map(new IllegalArgumentException(_)))

  /** ✅ 生成一个新的随机 DeviceId */
  def generate(): AuditLogId =
    AuditLogId(UUID.randomUUID())
