package app.mosia.nexus.domain.model.session

import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.resource.ResourceAssignment
import app.mosia.nexus.domain.model.simulation.SimulationId
import app.mosia.nexus.domain.model.user.UserId
import caliban.schema.Schema

import java.time.Instant

/** Session - 仿真运行实例 职责: 管理一次仿真的完整生命周期，从启动到结束
  */
case class NeuroSession(
  id: SessionId,
  simulationId: SimulationId,
  projectId: ProjectId, // 冗余，便于查询
  userId: UserId,

  // 配置快照 (防止 Simulation 被修改)
  configSnapshot: SimulationConfigSnapshot,

  // 运行时状态
  status: SessionStatus,
  resourceAssignment: Option[ResourceAssignment],
  // 实时指标 (运行时更新)
  metrics: Option[SessionMetrics], // 最新快照，从 PostgreSQL 读

  // 结果 (完成后写入)
  result: Option[SessionResult],

  // 时间戳
  createdAt: Instant,
  startedAt: Option[Instant],
  completedAt: Option[Instant]
):
  /** 启动会话 */
  def start(assignment: ResourceAssignment): NeuroSession =
    require(status == SessionStatus.Pending, "Can only start pending sessions")
    copy(
      status = SessionStatus.Running,
      resourceAssignment = Some(assignment),
      startedAt = Some(Instant.now())
    )

  /** 停止会话 */
  def stop(): NeuroSession =
    copy(
      status = SessionStatus.Stopped,
      completedAt = Some(Instant.now())
    )

  /** 更新指标 */
  def updateMetrics(newMetrics: SessionMetrics): NeuroSession =
    copy(metrics = Some(newMetrics))

  /** 完成会话 */
  def complete(result: SessionResult): NeuroSession =
    require(status == SessionStatus.Running, "Can only complete running sessions")
    copy(
      status = SessionStatus.Completed,
      result = Some(result),
      completedAt = Some(Instant.now())
    )

  /** 失败 */
  def fail(reason: String): NeuroSession =
    copy(
      status = SessionStatus.Failed,
      result = Some(SessionResult.failed(id, reason)),
      completedAt = Some(Instant.now())
    )

  /** 获取运行时长 (秒) */
  def duration: Option[Long] =
    for
      start <- startedAt
      end <- completedAt
    yield java.time.Duration.between(start, end).getSeconds
