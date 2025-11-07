package app.mosia.nexus.application.dto.response.common

import sttp.tapir.Schema
import zio.json.JsonCodec

case class ApiMeta(
  total: Option[Long] = None,
  page: Option[Int] = None,
  pageSize: Option[Int] = None
) derives JsonCodec,
      Schema
