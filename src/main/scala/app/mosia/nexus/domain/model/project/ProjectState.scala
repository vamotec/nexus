package app.mosia.nexus.domain.model.project

import java.time.Instant

sealed trait ProjectState:
  def edit(project: Project, name: String, description: Option[String]): Project

  def archive(project: Project): Project

  def canBeEdited: Boolean

  def isArchived: Boolean

case object Active extends ProjectState:
  def edit(project: Project, name: String, description: Option[String]): Project =
    project.copy(name = name, description = description)

  def archive(project: Project): Project =
    project.copy(state = Archived)

  def canBeEdited: Boolean = true
  def isArchived: Boolean  = false

case object Archived extends ProjectState:
  def edit(project: Project, name: String, description: Option[String]): Project =
    throw new IllegalStateException("Cannot edit an archived project")

  def archive(project: Project): Project = project // 已是归档

  def canBeEdited: Boolean = false
  def isArchived: Boolean  = true
