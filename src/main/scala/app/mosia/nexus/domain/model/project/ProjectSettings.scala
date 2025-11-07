package app.mosia.nexus.domain.model.project

case class ProjectSettings(
  defaultEnvironment: String,
  autoSaveResults: Boolean,
  retentionDays: Int
)
