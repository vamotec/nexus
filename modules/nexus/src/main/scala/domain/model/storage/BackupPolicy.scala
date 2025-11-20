package app.mosia.nexus
package domain.model.storage

/** 备份策略 */
enum BackupPolicy:
  case None, Daily, Weekly, Monthly, Continuous
