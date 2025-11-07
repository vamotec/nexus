package app.mosia.nexus.application.dto.response.session

import caliban.schema.{ArgBuilder, Schema}

case class MetricsResponse(fps: Double, frameCount: Long, gpuUtilization: Double, gpuMemoryMB: Long)
    derives Schema.SemiAuto,
      ArgBuilder
