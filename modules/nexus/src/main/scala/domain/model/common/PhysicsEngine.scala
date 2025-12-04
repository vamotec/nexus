package app.mosia.nexus
package domain.model.common

import domain.error.*

import sttp.tapir.Schema
import zio.json.*
import zio.*


enum PhysicsEngine extends ValueObject derives JsonCodec, Schema:
  case PhysX, Bullet

//object PhysicsEngine:
//  def fromString(str: String): Either[String, PhysicsEngine] =
//    Try(PhysicsEngine.valueOf(str)).toEither.left.map(_ => s"Invalid role: $str")
