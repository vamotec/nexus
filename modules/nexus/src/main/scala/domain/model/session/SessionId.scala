package app.mosia.nexus
package domain.model.session

import domain.error.AppTask
import domain.model.common.EntityId
import domain.model.user.UserId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

import java.util.UUID

case class SessionId(value: UUID) extends EntityId[SessionId] derives JsonCodec, Cs.SemiAuto, ArgBuilder

object SessionId:
  def generate(): SessionId = SessionId(UUID.randomUUID())

  def fromString(str: String): AppTask[SessionId] =
    EntityId.fromString(str)(using SessionId.apply)
