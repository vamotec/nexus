package app.mosia.nexus
package application.dto.response.metrics

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum IssueSeverity derives Cs.SemiAuto, ArgBuilder:
  case Critical
  case Warning
  case Info
