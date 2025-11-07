package app.mosia.nexus.application.dto.response.common

import app.mosia.nexus.infra.error.ErrorResponse
import sttp.tapir.Schema
import zio.json.JsonCodec

case class ApiResponse[T](
  data: T,
  meta: Option[ApiMeta] = None
) derives JsonCodec,
      Schema
