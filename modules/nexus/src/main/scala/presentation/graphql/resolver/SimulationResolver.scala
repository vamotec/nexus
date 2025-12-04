package app.mosia.nexus
package presentation.graphql.resolver

import domain.services.infra.JwtContent
import application.dto.mapper.SimulationMapper
import application.dto.request.simulation.CreateSimulationRequest
import application.dto.response.simulation.{SimulationListResponse, SimulationResponse}
import domain.error.*
import domain.model.project.ProjectId
import domain.model.simulation.{Simulation, SimulationId}
import domain.model.user.UserId
import domain.services.app.SimulationService
import presentation.graphql.schema.SimulationSchema.*

import zio.ZIO

/** Simulation GraphQL Resolver
  *
  * 负责将 GraphQL 查询/变更映射到 SimulationService
  *
  * 职责：
  *   - 查询仿真配置（单个/列表）
  *   - 创建、更新、删除仿真配置
  *   - 克隆仿真配置
  *   - Domain 模型与 DTO 之间的转换
  */
object SimulationResolver:

  /** 查询解析器 */
  def queries(service: SimulationService, jwtContent: JwtContent) = SimulationQueries(
    // 根据 ID 查询单个仿真配置（返回完整配置）
    simulation = simulationId =>
      (for
        simId <- SimulationId.fromString(simulationId)
        // 1. 从 service 获取仿真配置
        simulationOpt <- service.getSimulation(simId)

        // 2. 转换为 DTO
        responseOpt = simulationOpt.map(domainToSimulationResponse)
      yield responseOpt).mapError(_.toCalibanError),

    // 查询项目下的所有仿真配置（返回简化列表）
    simulationsByProject = projectId =>
      (for
        userIdStr <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(userIdStr)
        proId <- ProjectId.fromString(projectId)
        // 1. 获取项目的所有仿真配置
        // 使用分页查询，获取前 100 个（可以根据需要调整）
        result <- service.getSimulationPaged(
          userId = userId,
          projectId = proId,
          page = 1,
          pageSize = 100,
          sort = "created_desc",
          search = None
        )

        (simulations, total) = result

        // 2. 转换为简化 DTO
        responses = simulations.map(domainToSimulationListResponse)
      yield responses).mapError(_.toCalibanError)
  )

  /** 变更解析器 */
  def mutations(service: SimulationService, jwtContent: JwtContent) = SimulationMutations(
    // 创建仿真配置
    createSimulation = request =>
      (for
        userIdStr <- jwtContent.get.someOrFail(InvalidInput("access token", "Invalid payload"))
        userId <- UserId.fromString(userIdStr)
        projectId <- ProjectId.fromString(request.projectId)
        // 1. 调用 service 创建仿真
        simulation <- service.createSimulation(
          projectId = projectId,
          request = request,
          createdBy = userId
        )

        // 2. 转换为响应 DTO
        response = domainToSimulationResponse(simulation)
      yield response).mapError(_.toCalibanError),

    // 更新仿真配置
    updateSimulation = args =>
      (for
        simId <- SimulationId.fromString(args.simulationId)
        simulation <- service.updateSimulation(simId, args.request)
        // 转换为响应 DTO
        response = domainToSimulationResponse(simulation)
      yield response).mapError(_.toCalibanError),

    // 删除仿真配置
    deleteSimulation = simulationId =>
      (for
        simId <- SimulationId.fromString(simulationId)
        // 1. 调用 service 删除仿真
        _ <- service.deleteSimulation(simId)
      // 2. 返回成功
      yield true).mapError(_.toCalibanError),

    // 克隆仿真配置
    cloneSimulation = args =>
      (for
        simId <- SimulationId.fromString(args.simulationId)
        // 1. 获取源仿真配置
        sourceOpt <- service.getSimulation(simId)
        source <- ZIO
          .fromOption(sourceOpt)
          .orElseFail(NotFound("Simulation", args.simulationId))

        // 2. 创建克隆请求
        cloneRequest = CreateSimulationRequest(
          projectId = source.projectId.value.toString,
          name = args.name,
          description = source.description.map(d => s"$d (cloned)"),
          config = application.dto.model.simulation.SimulationConfigDto(
            sceneConfig = SimulationMapper.toSceneConfigDto(source.sceneConfig),
            simulationParams = source.simulationParams,
            trainingConfig = source.trainingConfig.map(_.toJsonAst),
            advancedConfig = None,
            metadata = None
          ),
          tags = source.tags :+ "cloned"
        )

        // 3. 调用 create
        cloned <- service.createSimulation(
          projectId = source.projectId,
          request = cloneRequest,
          createdBy = source.createdBy
        )

        // 4. 转换为响应
        response = domainToSimulationResponse(cloned)
      yield response).mapError(_.toCalibanError)
  )

  // ============================================================================
  // 辅助方法 - Domain → DTO 转换
  // ============================================================================

  /** Domain Simulation → SimulationResponse (完整响应) */
  private def domainToSimulationResponse(simulation: Simulation): SimulationResponse =
    SimulationResponse(
      simulationId = simulation.id.value.toString,
      name = simulation.name,
      description = simulation.description,
      sceneConfig = SimulationMapper.toSceneConfigDto(simulation.sceneConfig),
      createdAt = simulation.createdAt.toString
    )

  /** Domain Simulation → SimulationListResponse (简化列表响应) */
  private def domainToSimulationListResponse(simulation: Simulation): SimulationListResponse =
    SimulationListResponse(
      simulationId = simulation.id.value.toString,
      name = simulation.name,
      description = simulation.description,
      sceneName = simulation.sceneConfig.name,
      robotType = SimulationMapper.robotTypeToString(simulation.sceneConfig.robotType),
      createdAt = simulation.createdAt.toString
    )
