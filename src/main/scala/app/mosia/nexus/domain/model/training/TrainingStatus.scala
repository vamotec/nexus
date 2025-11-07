package app.mosia.nexus.domain.model.training

enum TrainingStatus:
  case Queued, Running, Paused, Completed, Failed, Cancelled
