package app.mosia.nexus.domain.model.project

import java.time.Instant

import app.mosia.nexus.domain.model.session.SessionId
import app.mosia.nexus.domain.model.training.ModelId
import app.mosia.nexus.domain.model.user.UserId

/** Project - 组织容器 职责: 管理相关的仿真配置，提供命名空间隔离
  */
case class Project(
  id: ProjectId,
  name: String,
  description: Option[String],
  ownerId: UserId,
  collaborators: List[UserId],
  tags: List[String],
  settings: ProjectSettings,
  state: ProjectState = Active,
  createdAt: Instant,
  updatedAt: Instant
):
  /** 添加协作者 */
  def addCollaborator(userId: UserId): Project =
    if (collaborators.contains(userId)) this
    else copy(collaborators = collaborators :+ userId, updatedAt = Instant.now())

  /** 更新设置 */
  def updateSettings(newSettings: ProjectSettings): Project =
    copy(settings = newSettings, updatedAt = Instant.now())

  def edit(name: String, description: Option[String] = None): Project =
    state.edit(this, name, description)

  def archive(): Project =
    state.archive(this)

  def canBeEdited: Boolean =
    state.canBeEdited

  def isArchived: Boolean =
    state.isArchived
