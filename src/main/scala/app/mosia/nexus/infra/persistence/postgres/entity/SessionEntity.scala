package app.mosia.nexus.infra.persistence.postgres.entity

import java.util.UUID

/** 数据库实体 - 使用扁平结构 */
case class SessionEntity(
  id: UUID,
  userId: UUID,
  projectId: UUID,
  sceneName: String,
  robotType: String,
  environment: String,
  status: String,
  // 资源分配信息 (JSON 存储或关联表)
  isaacSimInstanceId: Option[String],
  nucleusPath: Option[String],
  streamHost: Option[String],
  streamPort: Option[Int],
  controlWsUrl: Option[String],
  // 指标 (可以单独存到时序数据库)
  fps: Double,
  frameCount: Long,
  gpuUtilization: Double,
  gpuMemoryMB: Long,
  // 时间戳
  createdAt: Long,
//                          updatedAt: Long,
  startedAt: Option[Long],
  completedAt: Option[Long]
)
