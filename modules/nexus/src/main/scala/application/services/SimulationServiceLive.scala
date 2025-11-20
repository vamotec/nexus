package app.mosia.nexus
package application.services

import application.dto.mapper.SimulationMapper
import application.dto.request.simulation.{CreateSimulationRequest, UpdateSimulationRequest}
import domain.error.*
import domain.model.project.ProjectId
import domain.model.simulation.{Simulation, SimulationId, SimulationStatistics, SimulationVersion}
import domain.model.training.TrainingConfig
import domain.model.user.UserId
import domain.repository.SimulationRepository
import domain.services.app.SimulationService

import java.time.Instant

import zio.json.*
import zio.*

/** 仿真配置管理应用服务
  *
  * 职责：
  *   - 创建和管理仿真配置模板（可复用）
  *   - 协调 DTO 映射和 Domain 逻辑
  *   - 仿真统计信息聚合
  */
final class SimulationServiceLive(repo: SimulationRepository) extends SimulationService:

  /** 创建仿真配置 */
  override def createSimulation(
    projectId: ProjectId,
    request: CreateSimulationRequest,
    createdBy: UserId
  ): AppTask[Simulation] =
    (for
      // 1. 生成 SimulationId
      simulationId <- ZIO.succeed(SimulationId.generate())

      // 2. 将 DTO 转换为领域模型
      sceneConfig <- ZIO
        .fromEither(
          SimulationMapper.toSceneConfig(request.config.sceneConfig)
        )
        .mapError(err => new IllegalArgumentException(s"Invalid scene config: $err"))

      // 3. 解析训练配置（可选）
      trainingConfig = request.config.trainingConfig.flatMap { json =>
        json.as[TrainingConfig].toOption
      }

      // 4. 构建领域对象
      now        = Instant.now()
      simulation = Simulation(
        id = simulationId,
        projectId = projectId,
        name = request.name,
        description = request.description,
        version = SimulationVersion(1, 0), // 初始版本
        sceneConfig = sceneConfig,
        simulationParams = request.config.simulationParams,
        trainingConfig = trainingConfig,
        statistics = SimulationStatistics.empty, // 初始统计为空
        tags = request.tags,
        createdBy = createdBy,
        createdAt = now,
        updatedAt = now
      )

      // 5. 保存到数据库
//      _ <- repo.save(simulation)
      _ <- ZIO.logInfo(s"Attempting to save project: ${simulation.id.value}") *>
        repo
          .save(simulation)
          .tapError(err => ZIO.logError(s"Failed to save project: ${err.getMessage}"))
          .tap(_ => ZIO.logInfo(s"Successfully saved project: ${simulation.id.value}"))
    yield simulation).mapError(toAppError)

  /** 获取单个仿真配置 */
  override def getSimulation(simulationId: SimulationId): AppTask[Option[Simulation]] =
    repo.findById(simulationId)

  /** 分页查询仿真配置 */
  override def getSimulationPaged(
    userId: UserId,
    projectId: ProjectId,
    page: Int,
    pageSize: Int,
    sort: String,
    search: Option[String]
  ): UIO[(List[Simulation], Int)] =
    for
      // 获取所有仿真配置
      allSimulations <- repo.findByProjectId(projectId).orDie

      // 按搜索条件过滤
      filtered = search match
        case Some(query) =>
          val lowerQuery = query.toLowerCase
          allSimulations.filter { sim =>
            sim.name.toLowerCase.contains(lowerQuery) ||
            sim.description.exists(_.toLowerCase.contains(lowerQuery)) ||
            sim.tags.exists(_.toLowerCase.contains(lowerQuery))
          }
        case None => allSimulations

      // 排序
      sorted = sort.toLowerCase match
        case "name" | "name_asc" => filtered.sortBy(_.name)
        case "name_desc" => filtered.sortBy(_.name)(using Ordering[String].reverse)
        case "created" | "created_desc" => filtered.sortBy(_.createdAt.toEpochMilli)(using Ordering[Long].reverse)
        case "created_asc" => filtered.sortBy(_.createdAt.toEpochMilli)
        case "updated" | "updated_desc" => filtered.sortBy(_.updatedAt.toEpochMilli)(using Ordering[Long].reverse)
        case "updated_asc" => filtered.sortBy(_.updatedAt.toEpochMilli)
        case _ => filtered.sortBy(_.createdAt.toEpochMilli)(using Ordering[Long].reverse) // 默认按创建时间倒序

      // 分页
      total     = sorted.length
      offset    = (page - 1) * pageSize
      paginated = sorted.slice(offset, offset + pageSize)
    yield (paginated, total)

  /** 更新仿真配置 */
  override def updateSimulation(
    simulationId: SimulationId,
    request: UpdateSimulationRequest
  ): AppTask[Simulation] =
    (for
      // 1. 获取现有配置
      existingOpt <- repo.findById(simulationId)
      existing <- ZIO
        .fromOption(existingOpt)
        .orElseFail(new NoSuchElementException(s"Simulation not found: ${simulationId.value}"))

      // 2. 应用更新（只更新提供的字段）
      updatedName        = request.name.getOrElse(existing.name)
      updatedDescription = request.description.orElse(existing.description)
      updatedTags        = request.tags.map(_.toList).getOrElse(existing.tags)

      // 3. 更新场景配置（如果提供）
      updatedSceneConfig <- request.config match
        case Some(configDto) =>
          ZIO
            .fromEither(
              SimulationMapper.toSceneConfig(configDto.sceneConfig)
            )
            .mapError(err => new IllegalArgumentException(s"Invalid scene config: $err"))
        case None => ZIO.succeed(existing.sceneConfig)

      // 4. 更新仿真参数（如果提供）
      updatedParams = request.config
        .map(_.simulationParams)
        .getOrElse(existing.simulationParams)

      // 5. 更新训练配置（如果提供）
      updatedTrainingConfig = request.config
        .flatMap(_.trainingConfig)
        .flatMap(_.as[TrainingConfig].toOption)
        .orElse(existing.trainingConfig)

      // 6. 如果配置有变化，增加版本号
      shouldIncrementVersion = request.config.isDefined
      updatedVersion         =
        if shouldIncrementVersion then existing.version.increment()
        else existing.version

      // 7. 构建更新后的对象
      updated = existing.copy(
        name = updatedName,
        description = updatedDescription,
        version = updatedVersion,
        sceneConfig = updatedSceneConfig,
        simulationParams = updatedParams,
        trainingConfig = updatedTrainingConfig,
        tags = updatedTags,
        updatedAt = Instant.now()
      )

      // 8. 保存更新
      _ <- repo.update(updated)
    yield updated).mapError(toAppError)

  /** 删除仿真配置 */
  override def deleteSimulation(simulationId: SimulationId): AppTask[Unit] =
    (for
      // 1. 验证仿真配置存在
      existingOpt <- repo.findById(simulationId)
      _ <- ZIO
        .fromOption(existingOpt)
        .orElseFail(new NoSuchElementException(s"Simulation not found: ${simulationId.value}"))

      // 2. 删除
      // TODO: 可以添加级联删除或软删除逻辑
      // 可以检查是否有正在运行的 Session
      _ <- repo.delete(simulationId)
    yield ()).mapError(toAppError)

  /** 批量获取项目的仿真配置数量 */
  override def getSimulationCounts(
    projectIds: Seq[ProjectId],
    userId: UserId
  ): UIO[Map[ProjectId, Int]] =
    ZIO
      .foreachPar(projectIds) { projectId =>
        repo
          .findByProjectId(projectId)
          .map(sims => projectId -> sims.length)
          .orElse(ZIO.succeed(projectId -> 0))
      }
      .map(_.toMap)

  /** 批量获取项目的最后运行时间 */
  override def getLastRunTimes(
    projectIds: Seq[ProjectId],
    userId: UserId
  ): UIO[Map[ProjectId, Instant]] =
    ZIO
      .foreachPar(projectIds) { projectId =>
        repo
          .findByProjectId(projectId)
          .map { simulations =>
            val lastRunOpt = simulations
              .flatMap(_.statistics.lastRunAt)
              .maxByOption(_.toEpochMilli)
            lastRunOpt.map(projectId -> _)
          }
          .orElse(ZIO.succeed(None))
      }
      .map(_.flatten.toMap)

object SimulationServiceLive:
  val live: ZLayer[SimulationRepository, Nothing, SimulationService] =
    ZLayer.fromFunction(new SimulationServiceLive(_))
