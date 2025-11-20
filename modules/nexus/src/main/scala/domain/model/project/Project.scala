package app.mosia.nexus
package domain.model.project

import domain.model.project.ProjectState.Active
import domain.model.user.UserId

import java.time.Instant

/** Project - 组织容器 职责: 管理相关的仿真配置，提供命名空间隔离
  */
case class Project(
  id: ProjectId,
  name: ProjectName,
  description: Option[String],
  ownerId: UserId,
  tags: List[String],
  settings: ProjectSettings,
  state: ProjectState = Active,
  createdAt: Instant,
  updatedAt: Instant
):
  /** 添加协作者 */
//  def addCollaborator(userId: UserId): Project =
//    if (collaborators.contains(userId)) this
//    else copy(collaborators = collaborators :+ userId, updatedAt = Instant.now())

  /** 更新设置 */
  def updateSettings(newSettings: ProjectSettings): Project =
    copy(settings = newSettings, updatedAt = Instant.now())

  def edit(name: ProjectName, description: Option[String] = None): Project =
    state.edit(this, name, description)

  def archive(): Project =
    state.archive(this)

  def canBeEdited: Boolean =
    state.canBeEdited

  def isArchived: Boolean =
    state.isArchived
