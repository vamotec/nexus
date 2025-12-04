package app.mosia.nexus
package domain.model.simulation

import sttp.tapir.Schema
import zio.json.*

enum SimulationStatus derives JsonCodec, Schema:
  case Pending // 已创建，等待启动
  case Running // 运行中
  case Completed // 正常结束
  case Failed // 异常终止
  case Stopped // 手动停止
  case Cancelling // 正在取消（过渡态）

object SimulationStatus:
  def toString(status: SimulationStatus): String = status match
    case SimulationStatus.Pending => "pending"
    case SimulationStatus.Running => "running"
    case SimulationStatus.Completed => "completed"
    case SimulationStatus.Failed => "failed"
    case SimulationStatus.Stopped => "stopped"
    case SimulationStatus.Cancelling => "cancelling"

  def fromString(s: String): SimulationStatus = s.toLowerCase match
    case "pending" => SimulationStatus.Pending
    case "running" => SimulationStatus.Running
    case "completed" => SimulationStatus.Completed
    case "failed" => SimulationStatus.Failed
    case "stopped" => SimulationStatus.Stopped
    case "cancelling" => SimulationStatus.Cancelling
    case _ => throw new IllegalArgumentException(s"Unknown simulation status: $s")
