package app.mosia.nexus.domain.model.scene

import zio.json.JsonCodec

enum RobotType derives JsonCodec:
  case FrankaPanda, UR5, Kuka
  case Custom(name: String)
