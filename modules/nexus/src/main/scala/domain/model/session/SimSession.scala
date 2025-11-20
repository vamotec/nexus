package app.mosia.nexus
package domain.model.session

import domain.model.common.Position3D
import domain.model.project.ProjectId
import domain.model.resource.{ControlEndpoint, IsaacSimInstanceId, ResourceAssignment, StreamEndpoint}
import domain.model.simulation.SimulationId
import domain.model.user.UserId

import java.time.Instant

/** Session - 仿真运行实例
  *
  * 职责: 管理一次仿真的完整生命周期，从启动到结束
  *
  * @param mode
  *   会话模式（Manual/Training/Hybrid），决定资源分配和控制方式
  */
case class SimSession(
  id: SessionId,
  simulationId: SimulationId,
  projectId: ProjectId, // 冗余，便于查询
  userId: UserId,
  clusterId: String,
  mode: SessionMode, // 会话模式：Manual/Training/Hybrid

  // 配置快照 (防止 Simulation 被修改)
  configSnapshot: SimulationConfigSnapshot,

  // 运行时状态
  status: SessionStatus,
  error: Option[SessionError],
  resourceAssignment: Option[ResourceAssignment],

  // 结果 (完成后写入)
  result: Option[SessionResult],

  // 时间戳
  createdAt: Instant,
  startedAt: Option[Instant],
  completedAt: Option[Instant]
):
  /** 启动会话 */
  def start(assignment: ResourceAssignment): SimSession =
    require(status == SessionStatus.Pending, "Can only start pending sessions")
    copy(
      status = SessionStatus.Running,
      resourceAssignment = Some(assignment),
      startedAt = Some(Instant.now())
    )

  /** 停止会话 */
  def stop(): SimSession =
    copy(
      status = SessionStatus.Stopped,
      completedAt = Some(Instant.now())
    )

  /** 完成会话 */
  def complete(result: SessionResult): SimSession =
    require(status == SessionStatus.Running, "Can only complete running sessions")
    copy(
      status = SessionStatus.Completed,
      result = Some(result),
      completedAt = Some(Instant.now())
    )

  /** 失败 */
  def fail(reason: String): SimSession =
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
