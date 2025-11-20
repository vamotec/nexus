package app.mosia.nexus
package domain.model.common

import domain.error.*

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

import io.getquill.MappedEncoding
import scala.util.Try

enum PhysicsEngine extends ValueObject derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder:
  case PhysX, Bullet

object PhysicsEngine:
  def fromString(str: String): Either[String, PhysicsEngine] =
    Try(PhysicsEngine.valueOf(str)).toEither.left.map(_ => s"Invalid role: $str")
