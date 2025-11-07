package app.mosia.nexus.application.dto.model.scene

import app.mosia.nexus.domain.model.common.{Position3D, Quaternion}
import app.mosia.nexus.application.dto.{given_ArgBuilder_Json, given_Schema_Any_Json}
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.json.zio.*
import sttp.tapir.Schema
import zio.json.*
import zio.json.ast.Json

/** 传感器 DTO - 简化版本
  *
  * 用于 API 层的传感器配置，使用 JSON 处理复杂配置
  */
case class SensorDto(
  // 传感器类型 (扁平化枚举)
  sensorType: String, // "camera", "lidar", "depth", "imu", "force_torque"

  // 位置和朝向 (复用值对象)
  position: Position3D,
  orientation: Option[Quaternion] = Some(Quaternion(1.0, 0.0, 0.0, 0.0)),

  // 传感器配置 (使用 JSON 灵活处理不同类型传感器的配置)
  config: Option[Json] = None // 例如: {"resolution": [1920, 1080], "fov": 90}
) derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder
