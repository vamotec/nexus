package app.mosia.nexus.application.dto.response.scene

import caliban.schema.{ArgBuilder, Schema}

case class SceneResponse(name: String, robotType: String, environment: String, obstacleCount: Int)
    derives Schema.SemiAuto,
      ArgBuilder
