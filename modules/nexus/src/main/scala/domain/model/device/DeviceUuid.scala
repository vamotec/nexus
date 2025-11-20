package app.mosia.nexus
package domain.model.device

import domain.error.AppTask
import domain.model.common.EntityId

import zio.json.*
import zio.*
import zio.json.ast.Json

import java.util.UUID

case class DeviceUuid(value: UUID) extends EntityId[DeviceUuid] derives JsonCodec

object DeviceUuid:
  /** 从字符串解析 DeviceId */
  def fromString(str: String): AppTask[DeviceUuid] =
    EntityId.fromString(str)(using DeviceUuid.apply)

  /** ✅ 生成一个新的随机 DeviceId */
  def generate(): DeviceUuid = DeviceUuid(UUID.randomUUID())
