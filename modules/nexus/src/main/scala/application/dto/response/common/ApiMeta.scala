package app.mosia.nexus
package application.dto.response.common

import sttp.tapir.Schema
import zio.json.*

case class ApiMeta(
  total: Option[Long] = None,
  page: Option[Int] = None,
  @jsonField("page_size") pageSize: Option[Int] = None
) derives JsonCodec,
      Schema
