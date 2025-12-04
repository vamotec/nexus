package app.mosia.nexus
package application.dto.request.project

import sttp.tapir.Schema
import zio.json.*
import zio.*

/** 创建项目请求 DTO */
case class CreateProjectRequest(
  name: String,
  description: Option[String]
) derives JsonCodec,
      Schema
