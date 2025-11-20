package app.mosia.nexus
package application.dto.response.session

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class StreamEndpointResponse(
  host: String,
  port: Int,
  protocol: String,
  url: String // 完整的连接 URL
) derives Cs.SemiAuto,
      ArgBuilder
