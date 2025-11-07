package app.mosia.nexus.application.service.project

import app.mosia.nexus.domain.model.project.{Project, ProjectId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask
import zio.{IO, Task, UIO}

trait ProjectService:
  def getUserProjects(userId: UserId): AppTask[List[Project]]

  def createProject(name: String, description: Option[String], createdBy: UserId): AppTask[Project]

  def getProject(projectId: ProjectId, userId: UserId): AppTask[Project]

  def getUserProjectsPaged(
    userId: UserId,
    page: Int,
    pageSize: Int,
    sort: String,
    search: Option[String]
  ): UIO[(List[Project], Int)]
