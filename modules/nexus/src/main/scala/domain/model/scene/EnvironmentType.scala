package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.ValueObject

import io.getquill.MappedEncoding
import scala.util.Try
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum EnvironmentType extends ValueObject derives JsonCodec:
  case Warehouse, Factory, Laboratory, Outdoor
