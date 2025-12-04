package app.mosia.nexus
package domain.model.resource

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

import zio.json.*

case class IsaacSimInstanceId(value: UUID) extends EntityId[IsaacSimInstanceId] derives JsonCodec

object IsaacSimInstanceId:
  def generate(): IsaacSimInstanceId = IsaacSimInstanceId(UUID.randomUUID())

  def fromString(str: String): AppTask[IsaacSimInstanceId] =
    EntityId.fromString(str)(using IsaacSimInstanceId.apply)
