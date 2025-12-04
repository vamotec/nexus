package app.mosia.nexus
package application.services

import application.dto.response.project.{ProjectResponse, ProjectpagedResponse}
import domain.error.*
import domain.model.project.*
import domain.model.user.UserId
import domain.repository.ProjectRepository
import domain.services.app.ProjectService

import zio.*

import java.time.Instant
import java.util.UUID

final class ProjectServiceLive(repo: ProjectRepository) extends ProjectService:

  /** 创建新项目 */
  override def createProject(
    nameInput: String,
    description: Option[String],
    createdBy: UserId
  ): AppTask[Project] =
    for
      // 1. 验证项目名称
      name <- ProjectName
        .fromZIO(nameInput)
        .mapError(_ => InvalidInput("project name", "validate name format failed", Some(nameInput)))

      // 2. 创建项目领域模型
      now     = Instant.now()
      project = Project(
        id = ProjectId(UUID.randomUUID()),
        name = name,
        description = description.map(_.trim).filter(_.nonEmpty),
        ownerId = createdBy,
        tags = List.empty,
        settings = ProjectSettings(
          defaultEnvironment = "default",
          autoSaveResults = true,
          retentionDays = 30
        ),
        state = ProjectState.Active,
        createdAt = now,
        updatedAt = now
      )
      
      // 3. 持久化到数据库
      _ <- ZIO.logInfo(s"Attempting to save project: ${project.id}") *>
        repo
          .save(project)
          .tapError(err => ZIO.logError(s"Failed to save project: ${err.getMessage}"))
          .tap(_ => ZIO.logInfo(s"Successfully saved project: ${project.id}"))
    yield project

  /** 获取单个项目（验证权限） */
  override def getProject(projectId: ProjectId, userId: UserId): AppTask[Project] =
    for
      // 1. 查询项目
      projectOpt <- repo.findById(projectId)

      // 2. 验证项目存在
      project <- ZIO
        .fromOption(projectOpt)
        .orElseFail(NotFound("Project", projectId.value.toString))

      // 3. 验证权限（只有项目所有者可以访问）
      _ <- ZIO
        .fail(PermissionDenied("list", "projects"))
        .when(project.ownerId != userId)
    yield project

  /** 获取用户的所有项目 */
  override def getUserProjects(userId: UserId): AppTask[List[Project]] =
    repo.findByUserId(userId)

  /** 分页获取用户项目（带搜索和排序） */
  override def getUserProjectsPaged(
    userId: UserId,
    page: Int,
    pageSize: Int,
    sort: String,
    search: Option[String]
  ): UIO[ProjectpagedResponse] =
    (for
      // 1. 获取用户的所有项目
      allProjects <- repo.findByUserId(userId)

      // 2. 搜索过滤（如果提供了搜索词）
      filtered = search match
        case Some(searchTerm) if searchTerm.trim.nonEmpty =>
          val term = searchTerm.toLowerCase
          allProjects.filter { project =>
            project.name.value.toLowerCase.contains(term) ||
            project.description.exists(_.toLowerCase.contains(term)) ||
            project.tags.exists(_.toLowerCase.contains(term))
          }
        case _ => allProjects

      // 3. 排序
      sorted = sort.toLowerCase match
        case "name" | "name_asc" => filtered.sortBy(_.name.value)
        case "name_desc" => filtered.sortBy(_.name.value)(using Ordering[String].reverse)
        case "created" | "created_desc" => filtered.sortBy(_.createdAt.toEpochMilli)(using Ordering[Long].reverse)
        case "created_asc" => filtered.sortBy(_.createdAt.toEpochMilli)
        case "updated" | "updated_desc" => filtered.sortBy(_.updatedAt.toEpochMilli)(using Ordering[Long].reverse)
        case "updated_asc" => filtered.sortBy(_.updatedAt.toEpochMilli)
        case _ => filtered.sortBy(_.updatedAt.toEpochMilli)(using Ordering[Long].reverse)

      // 4. 分页
      total      = sorted.length
      startIndex = (page - 1) * pageSize
      paged      = sorted.slice(startIndex, startIndex + pageSize)

      // 5. 转换为响应 DTO
      responses = paged.map(project =>
        ProjectResponse(
          id = project.id.value.toString,
          name = project.name.value,
          description = project.description,
          createdAt = project.createdAt.toEpochMilli,
          updatedAt = Some(project.updatedAt.toEpochMilli),
          simulationCount = 0, // TODO: 从 SimulationService 获取
          lastRunAt = None, // TODO: 从 SessionService 获取
          status = Some(if project.isArchived then "archived" else "active"),
          tags = project.tags
        )
      )
    yield ProjectpagedResponse(
      projects = responses,
      total = total
    )).orDie // 转换为 UIO

object ProjectServiceLive:
  val live: ZLayer[ProjectRepository, Nothing, ProjectServiceLive] =
    ZLayer.fromFunction(new ProjectServiceLive(_))
