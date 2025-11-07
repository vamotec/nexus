package app.mosia.nexus.application.dto.request.project

import sttp.tapir.Schema
import zio.json.JsonCodec

case class CreateProjectRequest(
  name: String,
  description: Option[String]
) derives JsonCodec,
      Schema
