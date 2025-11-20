package app.mosia.nexus
package domain.event

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 项目活动类型 */
enum ActivityType derives JsonCodec, Cs.SemiAuto:
  case ProjectCreated // 项目创建
  case ProjectUpdated // 项目更新
  case ProjectArchived // 项目归档
  case SimulationCreated // 仿真配置创建
  case SimulationUpdated // 仿真配置更新
  case SimulationDeleted // 仿真配置删除
  case SessionStarted // 会话启动
  case SessionCompleted // 会话完成
  case SessionFailed // 会话失败
  case CollaboratorAdded // 协作者添加
  case CollaboratorRemoved // 协作者移除
