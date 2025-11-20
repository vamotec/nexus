package app.mosia.nexus
package domain.model.training

enum TrainingStatus:
  case Queued, Running, Paused, Completed, Failed, Cancelled

object TrainingStatus:
  def fromString(str: String): TrainingStatus =
    str.toLowerCase match
      case "queued" => TrainingStatus.Queued
      case "running" => TrainingStatus.Running
      case "paused" => TrainingStatus.Paused
      case "completed" => TrainingStatus.Completed
      case "failed" => TrainingStatus.Failed
      case "cancelled" => TrainingStatus.Cancelled
      case _ => throw new IllegalArgumentException(s"Invalid training status: $str")

  def toString(status: TrainingStatus): String = status match
    case TrainingStatus.Queued => "queued"
    case TrainingStatus.Running => "running"
    case TrainingStatus.Paused => "paused"
    case TrainingStatus.Completed => "completed"
    case TrainingStatus.Failed => "failed"
    case TrainingStatus.Cancelled => "cancelled"
