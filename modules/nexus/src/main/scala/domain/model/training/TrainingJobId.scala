package app.mosia.nexus
package domain.model.training

import domain.error.AppTask
import domain.model.common.EntityId

import java.util.UUID

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class TrainingJobId(value: UUID) extends EntityId[TrainingJobId] derives JsonCodec, Cs.SemiAuto, ArgBuilder

object TrainingJobId:
  def generate(): TrainingJobId = TrainingJobId(UUID.randomUUID())

  def fromString(str: String): AppTask[TrainingJobId] =
    EntityId.fromString(str)(using TrainingJobId.apply)
