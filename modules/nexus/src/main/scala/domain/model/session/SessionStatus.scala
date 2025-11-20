package app.mosia.nexus
package domain.model.session

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

enum SessionStatus derives JsonCodec, Cs.SemiAuto, ArgBuilder:
  case Pending, Initializing, Running, Paused, Stopped, Failed, Completed

  def isActive: Boolean = this match
    case Running | Paused => true
    case _ => false

object SessionStatus:
  def toString(status: SessionStatus): String = status match
    case SessionStatus.Pending => "pending"
    case SessionStatus.Initializing => "initializing"
    case SessionStatus.Running => "running"
    case SessionStatus.Paused => "paused"
    case SessionStatus.Stopped => "stopped"
    case SessionStatus.Failed => "failed"
    case SessionStatus.Completed => "completed"

  def fromString(s: String): SessionStatus = s.toLowerCase match
    case "pending" => SessionStatus.Pending
    case "initializing" => SessionStatus.Initializing
    case "running" => SessionStatus.Running
    case "paused" => SessionStatus.Paused
    case "stopped" => SessionStatus.Stopped
    case "failed" => SessionStatus.Failed
    case "completed" => SessionStatus.Completed
    case _ => throw new IllegalArgumentException(s"Unknown session status: $s")
