package app.mosia.nexus
package domain.services.app

import application.dto.response.project.ProjectpagedResponse
import domain.error.AppTask
import domain.model.project.{Project, ProjectId}
import domain.model.user.UserId
import zio.json.*
import zio.*

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
  ): UIO[ProjectpagedResponse]
