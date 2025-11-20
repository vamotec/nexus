package app.mosia.nexus
package domain.model.user

import domain.model.common.ValueObject
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class ResourceQuota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGB: Double,
  currentUsage: ResourceUsage
) extends ValueObject derives JsonCodec
