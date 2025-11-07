package app.mosia.nexus.domain.model.simulation

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

enum SimulationStatus derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder:
  case Pending // 已创建，等待启动
  case Running // 运行中
  case Completed // 正常结束
  case Failed // 异常终止
  case Stopped // 手动停止
  case Cancelling // 正在取消（过渡态）
