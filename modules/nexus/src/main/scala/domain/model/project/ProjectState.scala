package app.mosia.nexus
package domain.model.project

sealed trait ProjectState:
  def edit(project: Project, name: ProjectName, description: Option[String]): Project

  def archive(project: Project): Project

  def canBeEdited: Boolean

  def isArchived: Boolean

object ProjectState:
  case object Active extends ProjectState:
    def edit(project: Project, name: ProjectName, description: Option[String]): Project =
      project.copy(name = name, description = description)

    def archive(project: Project): Project =
      project.copy(state = Archived)

    def canBeEdited: Boolean = true
    def isArchived: Boolean  = false

  case object Archived extends ProjectState:
    def edit(project: Project, name: ProjectName, description: Option[String]): Project =
      throw new IllegalStateException("Cannot edit an archived project")

    def archive(project: Project): Project = project // 已是归档

    def canBeEdited: Boolean = false
    def isArchived: Boolean  = true
