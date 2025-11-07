package app.mosia.nexus.domain.model.user

import app.mosia.nexus.domain.model.common.ValueObject
import zio.json.JsonCodec

case class ResourceUsage(activeSessions: Int, gpuHoursThisMonth: Double, storageUsedGB: Double) extends ValueObject
    derives JsonCodec
