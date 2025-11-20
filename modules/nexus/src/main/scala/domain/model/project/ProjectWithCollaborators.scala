package app.mosia.nexus
package domain.model.project

case class ProjectWithCollaborators(
  project: Project,
  collaborators: List[Collaborator]
)
