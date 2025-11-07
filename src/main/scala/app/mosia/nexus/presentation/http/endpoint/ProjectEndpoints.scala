package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.application.dto.mapper.SimulationMapper
import app.mosia.nexus.application.dto.request.common.ListQuery
import app.mosia.nexus.application.dto.request.project.CreateProjectRequest
import app.mosia.nexus.application.dto.request.simulation.CreateSimulationRequest
import app.mosia.nexus.application.dto.response.common.{ApiMeta, ApiResponse}
import app.mosia.nexus.application.dto.response.project.ProjectResponse
import app.mosia.nexus.application.dto.response.simulation.SimulationResponse
import app.mosia.nexus.application.service.project.ProjectService
import app.mosia.nexus.application.service.simulation.SimulationService
import app.mosia.nexus.domain.model.project.ProjectId
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.error.ErrorMapper
import app.mosia.nexus.presentation.http.endpoint.SecureEndpoints.secureBase
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*

final class ProjectEndpoints(
  projectService: ProjectService,
  simulationService: SimulationService,
) extends EndpointModule:

  override def endpoints: List[ZServerEndpoint[JwtService, ZioStreams]] =
    List(getProjectsList, createProject, getProjectDetail, getSimulationsList, createSimulation)

  private val getProjectsList: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.get
      .in("api" / "v1" / "projects")
      .in(
        query[Int]("page")
          .default(1)
          .and(query[Int]("pageSize").default(20))
          .and(query[String]("sort").default("createdAt:desc"))
          .and(query[Option[String]]("search"))
          .mapTo[ListQuery]
      )
      .out(jsonBody[ApiResponse[List[ProjectResponse]]])
      .description("Get paginated project list with simulation statistics")
      .serverLogic { userId => query =>
        for
          (projects, total) <- projectService.getUserProjectsPaged(
            userId,
            query.page,
            query.pageSize,
            query.sort,
            query.search
          )

          projectIds = projects.map(_.id)
          simCounts <- simulationService.getSimulationCounts(projectIds, userId)
          lastRuns <- simulationService.getLastRunTimes(projectIds, userId)

          responses = projects.map { p =>
            ProjectResponse(
              id = p.id.value.toString,
              name = p.name,
              description = p.description,
              createdAt = p.createdAt.toEpochMilli,
              updatedAt = Some(p.updatedAt.toEpochMilli),
              simulationCount = simCounts.getOrElse(p.id, 0),
              lastRunAt = lastRuns.get(p.id).map(_.toEpochMilli)
            )
          }

          meta = ApiMeta(
            total = Some(total),
            page = Some(query.page),
            pageSize = Some(query.pageSize)
          )
        yield ApiResponse(data = responses, meta = Some(meta))
      }

  private val createProject: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.post
      .in("api" / "v1" / "projects")
      .in(jsonBody[CreateProjectRequest])
      .out(jsonBody[ApiResponse[ProjectResponse]])
      .description("Create new project for current user")
      .serverLogic { userId => request =>
        (for
          project <- projectService.createProject(
            name = request.name,
            description = request.description,
            createdBy = userId
          )
          response = ProjectResponse(
            id = project.id.value.toString,
            name = project.name,
            description = project.description,
            createdAt = project.createdAt.toEpochMilli,
            updatedAt = Some(project.updatedAt.toEpochMilli),
            simulationCount = 0,
            lastRunAt = None
          )
        yield ApiResponse(data = response)).mapError(ErrorMapper.toErrorResponse)
      }

  private val getProjectDetail: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.get
      .in("api" / "v1" / "projects" / path[String]("projectId"))
      .out(jsonBody[ApiResponse[ProjectResponse]])
      .description("Create new project for current user")
      .serverLogic { userId => projectId =>
        (for
          projectId <- ProjectId.fromStringZIO(projectId)
          project <- projectService.getProject(projectId, userId)
          simCounts <- simulationService.getSimulationCounts(Seq(projectId), userId)
          lastRuns <- simulationService.getLastRunTimes(Seq(projectId), userId)
          response = ProjectResponse(
            id = project.id.value.toString,
            name = project.name,
            description = project.description,
            createdAt = project.createdAt.toEpochMilli,
            updatedAt = Some(project.updatedAt.toEpochMilli),
            simulationCount = simCounts.getOrElse(projectId, 0),
            lastRunAt = lastRuns.get(projectId).map(_.toEpochMilli)
          )
        yield ApiResponse(data = response)).mapError(ErrorMapper.toErrorResponse)
      }

  private val getSimulationsList: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.get
      .in("api" / "v1" / "projects" / path[String]("projectId") / "simulations")
      .in(
        query[Int]("page")
          .default(1)
          .and(query[Int]("pageSize").default(20))
          .and(query[String]("sort").default("createdAt:desc"))
          .and(query[Option[String]]("search"))
          .mapTo[ListQuery]
      )
      .out(jsonBody[ApiResponse[List[SimulationResponse]]])
      .description("Get paginated simulations list")
      .serverLogic { userId => (projectId, query) =>
        (for
          projectId <- ProjectId.fromStringZIO(projectId)
          (simulations, total) <- simulationService.getSimulationPaged(
            userId,
            projectId,
            query.page,
            query.pageSize,
            query.sort,
            query.search
          )
          responses = simulations.map { s =>
            SimulationResponse(
              simulationId = s.id.value.toString,
              name = s.name,
              description = s.description,
              sceneConfig = SimulationMapper.toSceneConfigDto(s.sceneConfig),
              createdAt = s.createdAt.toString
            )
          }
          meta = ApiMeta(
            total = Some(total),
            page = Some(query.page),
            pageSize = Some(query.pageSize)
          )
        yield ApiResponse(data = responses, meta = Some(meta))).mapError(ErrorMapper.toErrorResponse)
      }

  private val createSimulation: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.post
      .in("api" / "v1" / "projects" / path[String]("projectId") / "simulations")
      .in(jsonBody[CreateSimulationRequest])
      .out(jsonBody[ApiResponse[SimulationResponse]])
      .description("Create new project for current user")
      .serverLogic { userId => (projectId, request) =>
        (for
          projectId <- ProjectId.fromStringZIO(projectId)
          simulation <- simulationService.createSimulation(
            projectId = projectId,
            request = request,
            createdBy = userId
          )
          scene    = SimulationMapper.toSceneConfigDto(simulation.sceneConfig)
          response = SimulationResponse(
            simulationId = simulation.id.value.toString,
            name = simulation.name,
            description = simulation.description,
            sceneConfig = scene,
            createdAt = simulation.createdAt.toString
          )
        yield ApiResponse(data = response)).mapError(ErrorMapper.toErrorResponse)
      }
