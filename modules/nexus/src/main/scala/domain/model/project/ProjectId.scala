package app.mosia.nexus
package domain.model.project

import domain.error.AppTask
import domain.model.common.EntityId

import sttp.tapir.Schema
import zio.json.*
import zio.*

import java.util.UUID

case class ProjectId(value: UUID) extends EntityId[ProjectId] derives JsonCodec, Schema

object ProjectId:
  def fromString(str: String): AppTask[ProjectId] =
    EntityId.fromString(str)(using ProjectId.apply)
