package app.mosia.nexus.domain.model.user

import zio.json.JsonCodec
import app.mosia.nexus.domain.model.common.EntityId
import zio.*

import java.util.UUID

case class UserId(value: UUID) extends EntityId[UserId] derives JsonCodec

object UserId:
  def fromString(str: String): Either[String, UserId] =
    EntityId.fromString(str)(UserId.apply)

  def fromStringZIO(str: String): IO[Throwable, UserId] =
    ZIO.fromEither(fromString(str).left.map(new IllegalArgumentException(_)))
