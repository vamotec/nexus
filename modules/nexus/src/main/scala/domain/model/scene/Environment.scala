package app.mosia.nexus
package domain.model.scene

import domain.error.*
import org.postgresql.util.PGobject

import scala.annotation.tailrec
import io.getquill.MappedEncoding
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class Environment(environmentType: EnvironmentType, lighting: LightingConfig, physics: PhysicsConfig)
    derives JsonCodec
