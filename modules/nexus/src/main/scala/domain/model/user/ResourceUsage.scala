package app.mosia.nexus
package domain.model.user

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ResourceUsage(activeSessions: Int, gpuHoursThisMonth: Double, storageUsedGB: Double) extends ValueObject
    derives JsonCodec
