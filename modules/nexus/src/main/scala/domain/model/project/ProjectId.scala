package app.mosia.nexus
package domain.model.project

import domain.error.AppTask
import domain.model.common.EntityId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

import java.util.UUID

case class ProjectId(value: UUID) extends EntityId[ProjectId] derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder

object ProjectId:
  def fromString(str: String): AppTask[ProjectId] =
    EntityId.fromString(str)(using ProjectId.apply)
