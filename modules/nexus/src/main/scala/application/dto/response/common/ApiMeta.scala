package app.mosia.nexus
package application.dto.response.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ApiMeta(
  total: Option[Long] = None,
  page: Option[Int] = None,
  @jsonField("page_size") pageSize: Option[Int] = None
) derives JsonCodec,
      Schema
