package app.mosia.nexus.domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

enum PhysicsEngine derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder:
  case PhysX, Bullet
