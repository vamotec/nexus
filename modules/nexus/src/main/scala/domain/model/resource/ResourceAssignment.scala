package app.mosia.nexus
package domain.model.resource

import domain.model.common.ValueObject

import java.time.Instant

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 资源分配 - 记录分配的计算资源 */
case class ResourceAssignment(
  isaacSimInstanceId: IsaacSimInstanceId,
  nucleusPath: String,
  streamEndpoint: StreamEndpoint,
  controlEndpoint: ControlEndpoint
) extends ValueObject derives JsonCodec
