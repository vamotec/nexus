package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.common.ValueObject
import zio.json.JsonCodec

import java.time.Instant

/** 资源分配 - 记录分配的计算资源 */
case class ResourceAssignment(
  isaacSimInstance: IsaacSimInstanceId,
  nucleusPath: String, // 场景在 Nucleus 上的路径
  streamEndpoint: StreamEndpoint,
  controlEndpoint: ControlEndpoint,
  assignedAt: Instant
) extends ValueObject derives JsonCodec
