package app.mosia.nexus
package domain.model.simulation

import domain.model.project.ProjectId
import domain.model.scene.*
import domain.model.session.SessionResult
import domain.model.training.TrainingConfig
import domain.model.user.UserId

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
