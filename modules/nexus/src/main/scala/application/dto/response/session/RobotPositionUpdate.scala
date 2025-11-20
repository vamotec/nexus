package app.mosia.nexus
package application.dto.response.session

import domain.model.common.{Quaternion, Vector3D}

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 机器人位置更新事件
  *
  * 用于实时推送机器人的位置和姿态信息
  */
case class RobotPositionUpdate(
  sessionId: String,
  robotId: String,

  // 位置信息
  position: Vector3D,

  // 姿态信息（四元数）
  orientation: Quaternion,

  // 运动状态
  linearVelocity: Vector3D,
  angularVelocity: Vector3D,

  // 时间戳
  timestamp: Long, // Unix timestamp (毫秒)
  simulationTime: Double // 仿真时间（秒）
) derives Cs.SemiAuto,
      JsonCodec
