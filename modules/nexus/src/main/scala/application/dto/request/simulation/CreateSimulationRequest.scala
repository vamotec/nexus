package app.mosia.nexus
package application.dto.request.simulation

import application.dto.model.simulation.SimulationConfigDto
import domain.model.project.ProjectId

import zio.json.*
import zio.*
import zio.json.ast.Json

/** 创建仿真请求 DTO
  *
  * 用于创建新的仿真配置模板
  */
case class CreateSimulationRequest(
  // 所属项目
  projectId: String,

  // 基本信息
  name: String,
  description: Option[String] = None,

  // 完整配置
  config: SimulationConfigDto,

  // 标签 (用于分类和搜索)
  tags: List[String] = List.empty
) derives JsonCodec
