package app.mosia.nexus.domain.repository

import app.mosia.nexus.domain.model.project.{Project, ProjectId}
import app.mosia.nexus.domain.model.user.UserId
import app.mosia.nexus.infra.error.AppTask

trait ProjectRepository:
  def save(project: Project): AppTask[Unit]

  def findById(id: ProjectId): AppTask[Option[Project]]

  def findByUserId(userId: UserId): AppTask[List[Project]]

  def update(project: Project): AppTask[Unit]

  def delete(id: ProjectId): AppTask[Unit]
