package app.mosia.nexus
package domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class StringTimeRange(start: String, end: String) derives Cs.SemiAuto, ArgBuilder
