package app.mosia.nexus
package application.dto.response.common

import sttp.tapir.Schema
import zio.json.*

case class ApiResponse[T](
  data: T,
  meta: Option[ApiMeta] = None
) derives JsonCodec,
      Schema
