package app.mosia.nexus
package application.dto.request.project

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 创建项目请求 DTO */
case class CreateProjectRequest(
  name: String,
  description: Option[String]
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
