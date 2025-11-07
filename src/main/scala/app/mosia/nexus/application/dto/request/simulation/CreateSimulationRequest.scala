package app.mosia.nexus.application.dto.request.simulation

import app.mosia.nexus.application.dto.model.simulation.SimulationConfigDto
import app.mosia.nexus.domain.model.project.ProjectId
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

/** 创建仿真请求 DTO
  *
  * 用于创建新的仿真配置模板
  */
case class CreateSimulationRequest(
  // 所属项目
  projectId: ProjectId,

  // 基本信息
  name: String,
  description: Option[String] = None,

  // 完整配置
  config: SimulationConfigDto,

  // 标签 (用于分类和搜索)
  tags: List[String] = List.empty
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
