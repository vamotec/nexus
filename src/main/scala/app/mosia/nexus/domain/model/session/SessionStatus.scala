package app.mosia.nexus.domain.model.session

import caliban.schema.{ArgBuilder, Schema}
import zio.json.JsonCodec

enum SessionStatus derives JsonCodec, Schema.SemiAuto, ArgBuilder:
  case Pending, Initializing, Running, Paused, Stopped, Failed, Completed

  def isActive: Boolean = this match
    case Running | Paused => true
    case _ => false
