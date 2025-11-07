package app.mosia.nexus.domain.model.project

import app.mosia.nexus.domain.model.common.EntityId
import app.mosia.nexus.infra.error.*
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.{IO, ZIO}
import zio.json.JsonCodec

import java.util.UUID

case class ProjectId(value: UUID) extends EntityId[ProjectId] derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder

object ProjectId:
  def fromString(str: String): Either[String, ProjectId] =
    EntityId.fromString(str)(ProjectId.apply)

  def fromStringZIO(str: String): IO[AppError, ProjectId] =
    ZIO.fromEither(fromString(str).left.map { errorMsg =>
      ValidationError.InvalidInput("projectId", errorMsg)
    })
