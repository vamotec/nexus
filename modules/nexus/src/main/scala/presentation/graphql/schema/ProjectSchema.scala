package app.mosia.nexus
package presentation.graphql.schema

import application.dto.request.project.{CreateProjectRequest, UpdateProjectRequest}
import application.dto.response.project.ProjectResponse
import domain.error.CalTask
import domain.event.ProjectActivityEvent
import domain.model.project.{Project, ProjectId}
import domain.model.user.UserId

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json
import zio.stream.ZStream

object ProjectSchema:
  case class ProjectQueries(
    project: String => CalTask[ProjectResponse],
    myProjects: Boolean => CalTask[List[ProjectResponse]]
  ) derives Cs.SemiAuto

  case class UpdateProjectArgs(
    projectId: String,
    request: UpdateProjectRequest
  ) derives Cs.SemiAuto,
        ArgBuilder

  case class ProjectMutations(
    createProject: CreateProjectRequest => CalTask[ProjectResponse],
    updateProject: UpdateProjectArgs => CalTask[ProjectResponse],
    deleteProject: String => CalTask[Boolean]
  ) derives Cs.SemiAuto

  /** GraphQL 订阅 */
  case class ProjectSubscriptions(
    // 项目活动订阅（项目内所有活动事件）
    projectActivity: String => ZStream[Any, Throwable, ProjectActivityEvent]
  ) derives Cs.SemiAuto
