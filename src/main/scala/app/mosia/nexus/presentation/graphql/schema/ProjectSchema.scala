package app.mosia.nexus.presentation.graphql.schema

import app.mosia.nexus.application.dto.request.project.{CreateProjectRequest, UpdateProjectRequest}
import app.mosia.nexus.application.dto.response.project.ProjectResponse
import app.mosia.nexus.domain.model.project.{Project, ProjectId}
import app.mosia.nexus.domain.model.user.UserId
import zio.Task

object ProjectSchema:

  case class Queries(
    project: ProjectId => Task[Option[ProjectResponse]],
    myProject: UserId => Task[List[ProjectResponse]]
  )

  case class Mutations(
    create: CreateProjectRequest => Task[ProjectResponse],
    update: (ProjectId, UpdateProjectRequest) => Task[ProjectResponse],
    delete: ProjectId => Task[Boolean]
  )
