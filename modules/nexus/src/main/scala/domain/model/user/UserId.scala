package app.mosia.nexus
package domain.model.user

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class UserId(value: UUID) extends EntityId[UserId] derives JsonCodec, Cs.SemiAuto, ArgBuilder

object UserId:
  def generate(): UserId = UserId(UUID.randomUUID())

  def fromString(str: String): AppTask[UserId] =
    EntityId.fromString(str)(using UserId.apply)
