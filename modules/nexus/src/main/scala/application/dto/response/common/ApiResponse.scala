package app.mosia.nexus
package application.dto.response.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ApiResponse[T](
  data: T,
  meta: Option[ApiMeta] = None
) derives JsonCodec,
      Schema
