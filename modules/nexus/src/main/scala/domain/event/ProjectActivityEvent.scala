package app.mosia.nexus
package domain.event

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 项目活动事件
  *
  * 用于实时推送项目相关的活动信息
  */
case class ProjectActivityEvent(
  projectId: String,
  activityType: ActivityType,
  actorId: String, // 执行操作的用户 ID
  actorName: String, // 执行操作的用户名称

  // 关联实体信息
  entityId: Option[String], // 关联的实体 ID（如 simulationId, sessionId）
  entityName: Option[String], // 关联的实体名称

  // 活动描述
  description: String,

  // 元数据（可选，用于存储额外的活动详情）
  metadata: Option[Map[String, String]],

  // 时间戳
  timestamp: Long // Unix timestamp (毫秒)
) derives Cs.SemiAuto,
      JsonCodec
