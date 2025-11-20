package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.project.{Project, ProjectId}
import domain.model.user.UserId

trait ProjectRepository:
  def save(project: Project): AppTask[Unit]

  def findById(id: ProjectId): AppTask[Option[Project]]

  def findByUserId(userId: UserId): AppTask[List[Project]]

  def update(project: Project): AppTask[Unit]

  def delete(id: ProjectId): AppTask[Unit]
