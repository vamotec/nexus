package app.mosia.nexus.domain.model.session

import zio.json.JsonCodec

import java.util.UUID
import app.mosia.nexus.domain.model.common.EntityId
import caliban.schema.{ArgBuilder, Schema}

case class SessionId(value: UUID) extends EntityId[SessionId] derives JsonCodec, Schema.SemiAuto, ArgBuilder

object SessionId:
  def generate(): SessionId = SessionId(UUID.randomUUID())

  def fromString(str: String): Option[SessionId] =
    try Some(SessionId(UUID.fromString(str)))
    catch case _: IllegalArgumentException => None
