package app.mosia.nexus.domain.model.storage

/** 备份策略 */
enum BackupPolicy:
  case None, Daily, Weekly, Monthly, Continuous
