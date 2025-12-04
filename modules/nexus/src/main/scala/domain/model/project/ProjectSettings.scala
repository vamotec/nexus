package app.mosia.nexus
package domain.model.project

import sttp.tapir.Schema
import zio.json.*
import zio.json.ast.Json

case class ProjectSettings(
  defaultEnvironment: String,
  autoSaveResults: Boolean,
  retentionDays: Int,
  requiredGpu: Boolean = false,
  requiredMemoryGb: Int = 8,
  preferredRegions: List[String] = Nil // ← 用户可以设置偏好
) derives JsonCodec,
      Schema

object ProjectSettings:
  extension (settings: ProjectSettings)
    def toJsonAst: Json =
      Json.decoder.decodeJson(settings.toJson).getOrElse(Json.Obj())

  def fromJsonAst(json: Json): ProjectSettings =
    json.toJson.fromJson[ProjectSettings].getOrElse(throw new RuntimeException("Invalid project settings JSON"))
