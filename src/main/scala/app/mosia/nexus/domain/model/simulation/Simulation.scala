package app.mosia.nexus.domain.model.simulation

import app.mosia.nexus.domain.model.common.Learning.TrainingConfig
import app.mosia.nexus.domain.model.common.TemplateId
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.domain.model.scene.SceneConfig
import app.mosia.nexus.domain.model.session.SessionResult
import app.mosia.nexus.domain.model.simulation.SimulationStatus.*
import app.mosia.nexus.domain.model.user.UserId

import java.time.Instant

/** Simulation - 仿真配置/模板 职责: 定义可重复运行的仿真配置，作为 Session 的蓝图
  */
case class Simulation(
  id: SimulationId,
  projectId: ProjectId,
  name: String,
  description: Option[String],
  version: SimulationVersion,

  // 配置 (不可变部分)
  sceneConfig: SceneConfig,
  simulationParams: SimulationParams,
  trainingConfig: Option[TrainingConfig],

  // 运行时状态
  status: SimulationStatus, // 新增：核心！
  startedAt: Option[Instant], // 启动时间
  endedAt: Option[Instant], // 结束时间（Completed/Failed/Stopped）
  failureReason: Option[String], // 失败原因

  // 统计信息 (聚合所有 Session 的结果)
  statistics: SimulationStatistics,

  // 元数据
  tags: List[String],
  createdBy: UserId,
  createdAt: Instant,
  updatedAt: Instant
):
  /** 创建新版本 */
  def createNewVersion(
    newSceneConfig: SceneConfig,
    newParams: SimulationParams
  ): Simulation = copy(
    version = version.increment(),
    sceneConfig = newSceneConfig,
    simulationParams = newParams,
    updatedAt = Instant.now()
  )

  /** 更新统计信息 (当 Session 完成时调用) */
  def updateStatistics(sessionResult: SessionResult): Simulation = {
    val newStats = statistics.addSessionResult(sessionResult)
    copy(statistics = newStats, updatedAt = Instant.now())
  }

  /** 是否可以开始新会话 */
  def canStartNewSession: Boolean = true // 可以添加限制逻辑

object Simulation:
  extension (sim: Simulation)
    def start(now: Instant = Instant.now()): Simulation =
      sim.status match
        case Pending =>
          sim.copy(
            status = SimulationStatus.Running,
            startedAt = Some(now),
            updatedAt = now
          )
        case _ => throw IllegalStateException(s"Cannot start simulation in state: ${sim.status}")

    def complete(now: Instant = Instant.now()): Simulation =
      sim.status match
        case Running =>
          sim.copy(
            status = SimulationStatus.Completed,
            endedAt = Some(now),
            updatedAt = now
          )
        case _ => throw IllegalStateException(s"Cannot complete simulation in state: ${sim.status}")

    def fail(reason: String, now: Instant = Instant.now()): Simulation =
      sim.status match
        case Running | Cancelling =>
          sim.copy(
            status = SimulationStatus.Failed,
            failureReason = Some(reason),
            endedAt = Some(now),
            updatedAt = now
          )
        case _ => throw IllegalStateException(s"Cannot fail simulation in state: ${sim.status}")

    def stop(now: Instant = Instant.now()): Simulation =
      sim.status match
        case Running =>
          sim.copy(
            status = SimulationStatus.Stopped,
            endedAt = Some(now),
            updatedAt = now
          )
        case _ => throw IllegalStateException(s"Cannot stop simulation in state: ${sim.status}")

    def isRunning: Boolean    = sim.status == SimulationStatus.Running
    def isTerminated: Boolean = Set(Completed, Failed, Stopped)(sim.status)
