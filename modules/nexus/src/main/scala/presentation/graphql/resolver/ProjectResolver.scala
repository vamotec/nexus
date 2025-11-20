package app.mosia.nexus
package presentation.graphql.resolver

import domain.services.infra.JwtContent
import application.dto.response.project.ProjectResponse
import domain.error.*
import domain.event.{ActivityType, ProjectActivityEvent}
import domain.model.project.*
import domain.model.user.UserId
import domain.services.app.{ProjectService, SimulationService}
import presentation.graphql.schema.ProjectSchema.*

import zio.{ZIO, Duration}
import zio.stream.ZStream

import java.time.Instant

/** Project GraphQL Resolver
  *
  * 负责将 GraphQL 查询/变更映射到 ProjectService
  *
  * 职责：
  *   - 查询项目信息（单个/列表）
  *   - 创建、更新、删除项目
  *   - 聚合项目统计信息（仿真数量、最后运行时间等）
  */
object ProjectResolver:

  /** 查询解析器 */
  def queries(
    projectService: ProjectService,
    simulationService: SimulationService,
    jwtContent: JwtContent
  ) = ProjectQueries(
    // 根据 ID 查询单个项目
    project = projectId =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)
        projectId <- ProjectId.fromString(projectId)
        // 1. 获取项目基本信息
        project <- projectService.getProject(projectId, userId)

        // 2. 聚合统计信息
        simulationCount <- simulationService
          .getSimulationCounts(Seq(projectId), userId)
          .map(_.getOrElse(projectId, 0))

        lastRunAt <- simulationService
          .getLastRunTimes(Seq(projectId), userId)
          .map(_.get(projectId).map(_.toEpochMilli))

        // 3. 构建响应
        response = ProjectResponse(
          id = project.id.value.toString,
          name = project.name.value,
          description = project.description,
          createdAt = project.createdAt.toEpochMilli,
          updatedAt = Some(project.updatedAt.toEpochMilli),
          simulationCount = simulationCount,
          lastRunAt = lastRunAt,
          status = Some(projectStateToString(project.state)),
          tags = project.tags
        )
      yield response).mapError(_.toCalibanError),

    // 查询当前用户的所有项目
    myProjects = includeArchived =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)
        // 1. 查询用户的所有项目
        projects <- projectService.getUserProjects(userId)

        // 2. 过滤归档项目（如果需要）
        filtered =
          if includeArchived then projects
          else projects.filter(!_.isArchived)

        // 3. 批量获取统计信息
        projectIds = filtered.map(_.id)
        simulationCounts <- simulationService.getSimulationCounts(projectIds, userId)
        lastRunTimes <- simulationService.getLastRunTimes(projectIds, userId)

        // 4. 构建响应列表
        responses = filtered.map { project =>
          ProjectResponse(
            id = project.id.value.toString,
            name = project.name.value,
            description = project.description,
            createdAt = project.createdAt.toEpochMilli,
            updatedAt = Some(project.updatedAt.toEpochMilli),
            simulationCount = simulationCounts.getOrElse(project.id, 0),
            lastRunAt = lastRunTimes.get(project.id).map(_.toEpochMilli),
            status = Some(projectStateToString(project.state)),
            tags = project.tags
          )
        }
      yield responses).mapError(_.toCalibanError)
  )

  /** 变更解析器 */
  def mutations(
    projectService: ProjectService,
    simulationService: SimulationService,
    jwtContent: JwtContent
  ) = ProjectMutations(
    // 创建项目
    createProject = request =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)
        // 1. 创建项目
        project <- projectService.createProject(
          name = request.name,
          description = request.description,
          createdBy = userId
        )

        // 2. 构建响应（新项目没有统计信息）
        response = ProjectResponse(
          id = project.id.value.toString,
          name = project.name.value,
          description = project.description,
          createdAt = project.createdAt.toEpochMilli,
          updatedAt = Some(project.updatedAt.toEpochMilli),
          simulationCount = 0,
          lastRunAt = None,
          status = Some(projectStateToString(project.state)),
          tags = project.tags
        )
      yield response).mapError(_.toCalibanError),

    // 更新项目
    updateProject = args =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)
        projectId <- ProjectId.fromString(args.projectId)
        // 1. 获取现有项目
        project <- projectService.getProject(projectId, userId)

        // 2. 应用更新
        updatedProject = applyProjectUpdates(project, args.request)

        // 3. TODO: 保存更新（需要在 ProjectService 添加 updateProject 方法）
//        _ <- projectService.updateProject(updatedProject)

        // 4. 获取统计信息
        simulationCount <- simulationService
          .getSimulationCounts(Seq(projectId), userId)
          .map(_.getOrElse(projectId, 0))

        lastRunAt <- simulationService
          .getLastRunTimes(Seq(projectId), userId)
          .map(_.get(projectId).map(_.toEpochMilli))

        // 5. 构建响应
        response = ProjectResponse(
          id = updatedProject.id.value.toString,
          name = updatedProject.name.value,
          description = updatedProject.description,
          createdAt = updatedProject.createdAt.toEpochMilli,
          updatedAt = Some(updatedProject.updatedAt.toEpochMilli),
          simulationCount = simulationCount,
          lastRunAt = lastRunAt,
          status = Some(projectStateToString(updatedProject.state)),
          tags = updatedProject.tags
        )
      yield response).mapError(_.toCalibanError),

    // 删除项目
    deleteProject = projectId =>
      (for
        payload <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(payload.userIdStr)
        projectId <- ProjectId.fromString(projectId)
        // 1. 验证项目存在且用户有权限
        _ <- projectService.getProject(projectId, userId)

        // 2. TODO: 删除项目（需要在 ProjectService 添加 deleteProject 方法）
        // result <- projectService.deleteProject(args.projectId, args.userId)

        // 暂时返回成功
        result = true
      yield result).mapError(_.toCalibanError)
  )

  // ============================================================================
  // 辅助方法
  // ============================================================================

  /** 应用项目更新 */
  private def applyProjectUpdates(
    project: domain.model.project.Project,
    request: application.dto.request.project.UpdateProjectRequest
  ): domain.model.project.Project =
    var updated = project

    // 更新 name（如果提供）
    request.name.foreach { nameInput =>
      updated = ProjectName.from(nameInput) match {
        case Right(validName) =>
          updated.copy(name = validName)
        case Left(_) =>
          throw InvalidInput("Project name", nameInput)
      }
    }

    // 更新 description（如果提供）
    request.description.foreach { desc =>
      updated = updated.copy(description = Some(desc))
    }

    // 更新标签
    request.tags.foreach { tags =>
      updated = updated.copy(tags = tags, updatedAt = Instant.now())
    }

    // 归档/取消归档
    request.archived.foreach { archived =>
      updated = if archived then updated.archive() else updated
    }

    updated

  /** ProjectState → 字符串 */
  private def projectStateToString(state: ProjectState): String =
    state match
      case ProjectState.Active => "active"
      case ProjectState.Archived => "archived"

  /** 订阅解析器 */
  def subscriptions(
//    projectService: ProjectService,
//    simulationService: SimulationService,
//    jwtContent: JwtContent
  ) = ProjectSubscriptions(
    // 项目活动订阅
    projectActivity = projectId =>
      // 实际应该从 Kafka topic 或 Redis Pub/Sub 读取项目活动事件
      // 这里使用 Mock 实现演示流式数据
      ZStream
        .repeatZIOWithSchedule(
          ZIO.succeed(generateMockProjectActivity(projectId)),
          zio.Schedule.spaced(Duration.fromSeconds(5))
        )
  )

  // ============================================================================
  // Mock 实现
  // ============================================================================

  /** 生成模拟的项目活动事件
    *
    * 实际实现应该：
    *   1. 从 Kafka 消费项目事件
    *   2. 从 Redis Pub/Sub 订阅项目通道
    *   3. 根据权限过滤事件
    */
  private def generateMockProjectActivity(projectId: String): ProjectActivityEvent =
    val activityTypes = Array(
      ActivityType.SimulationCreated,
      ActivityType.SimulationUpdated,
      ActivityType.SessionStarted,
      ActivityType.SessionCompleted
    )

    val randomType = activityTypes(scala.util.Random.nextInt(activityTypes.length))

    ProjectActivityEvent(
      projectId = projectId,
      activityType = randomType,
      actorId = "user-001",
      actorName = "Test User",
      entityId = Some(s"entity-${scala.util.Random.nextInt(1000)}"),
      entityName = Some(s"Test Entity ${scala.util.Random.nextInt(100)}"),
      description = generateActivityDescription(randomType),
      metadata = Some(
        Map(
          "source" -> "graphql-subscription",
          "mock" -> "true"
        )
      ),
      timestamp = System.currentTimeMillis()
    )

  private def generateActivityDescription(activityType: ActivityType): String =
    activityType match
      case ActivityType.ProjectCreated => "Project was created"
      case ActivityType.ProjectUpdated => "Project settings were updated"
      case ActivityType.ProjectArchived => "Project was archived"
      case ActivityType.SimulationCreated => "New simulation configuration created"
      case ActivityType.SimulationUpdated => "Simulation configuration updated"
      case ActivityType.SimulationDeleted => "Simulation configuration deleted"
      case ActivityType.SessionStarted => "New simulation session started"
      case ActivityType.SessionCompleted => "Simulation session completed successfully"
      case ActivityType.SessionFailed => "Simulation session failed"
      case ActivityType.CollaboratorAdded => "Collaborator added to project"
      case ActivityType.CollaboratorRemoved => "Collaborator removed from project"
