package app.mosia.nexus
package application.dto.response.project

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ProjectpagedResponse(
  projects: List[ProjectResponse],
  total: Int
) derives JsonCodec,
      Schema
