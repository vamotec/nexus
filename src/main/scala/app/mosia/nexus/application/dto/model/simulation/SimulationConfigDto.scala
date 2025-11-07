package app.mosia.nexus.application.dto.model.simulation

import app.mosia.nexus.application.dto.model.scene.SceneConfigDto
import app.mosia.nexus.domain.model.simulation.SimulationParams
import app.mosia.nexus.application.dto.{given_ArgBuilder_Json, given_Schema_Any_Json}
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import sttp.tapir.json.zio.*
import zio.json.ast.Json
import zio.json.*

/** 仿真配置 DTO - 用于创建仿真时的完整配置
  *
  * 采用混合策略的纯数据传输对象：
  *   - 使用简化的 SceneConfigDto（而非完整的 Domain SceneConfig）
  *   - 直接复用稳定的 SimulationParams
  *   - 训练配置使用 JSON（灵活处理不同算法）
  *   - 支持高级配置扩展
  */
case class SimulationConfigDto(
  // 场景配置 (简化 DTO)
  sceneConfig: SceneConfigDto,

  // 仿真参数 (复用 Domain 稳定配置)
  simulationParams: SimulationParams,

  // 训练配置 (使用 JSON 灵活处理)
  trainingConfig: Option[Json] = None,
  // 例如: {"algorithm": "PPO", "episodes": 1000, "learningRate": 0.001}

  // 高级配置 (可选，用于扩展功能)
  advancedConfig: Option[Json] = None,
  // 例如: {"parallel": true, "workers": 4, "checkpoint": "path/to/model"}

  // 元数据 (可选)
  metadata: Option[Map[String, String]] = None
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
