package app.mosia.nexus.domain.model.device

import app.mosia.nexus.domain.model.common.EntityId
import zio.json.JsonCodec

import java.util.UUID

case class DeviceId(value: UUID) extends EntityId[DeviceId] derives JsonCodec

object DeviceId:
  /** 从字符串解析 DeviceId */
  def fromString(str: String): Either[String, DeviceId] =
    EntityId.fromString(str)(DeviceId.apply)

  /** ZIO 版本的 fromString */
  def fromStringZIO(str: String): zio.IO[Throwable, DeviceId] =
    zio.ZIO.fromEither(fromString(str).left.map(new IllegalArgumentException(_)))

  /** ✅ 生成一个新的随机 DeviceId */
  def generate(): DeviceId =
    DeviceId(UUID.randomUUID())
