package app.mosia.nexus
package application.dto.request.simulation

import application.dto.model.simulation.SimulationConfigDto
import domain.model.simulation.SimulationStatus

import zio.json.*
import zio.*

/** 更新仿真请求 DTO */
case class UpdateSimulationRequest(
  // 基本信息更新
  name: Option[String] = None,
  description: Option[String] = None,

  // 环境配置更新
  environment: Option[String] = None,

  // 配置更新 - 使用与创建时相同的完整配置，但所有字段都是可选的
  config: Option[SimulationConfigDto] = None,

  // 变量更新
  variables: Option[Map[String, String]] = None,

  // 状态更新（用于暂停、恢复等操作）
  status: Option[SimulationStatus] = None,

  // 标签和元数据
  tags: Option[Set[String]] = None,
  metadata: Option[Map[String, String]] = None
) derives JsonCodec
