package app.mosia.nexus
package application.dto.request.project

import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 更新项目请求 DTO
  *
  * 所有字段都是可选的，只更新提供的字段
  */
case class UpdateProjectRequest(
  name: Option[String] = None,
  description: Option[String] = None,
  tags: Option[List[String]] = None,
  archived: Option[Boolean] = None // 是否归档
) derives JsonCodec,
      Schema
