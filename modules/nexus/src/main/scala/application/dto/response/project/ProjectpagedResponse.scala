package app.mosia.nexus
package application.dto.response.project

import sttp.tapir.Schema
import zio.json.*
import zio.*

case class ProjectpagedResponse(
  projects: List[ProjectResponse],
  total: Int
) derives JsonCodec,
      Schema
