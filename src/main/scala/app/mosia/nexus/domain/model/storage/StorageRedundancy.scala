package app.mosia.nexus.domain.model.storage

/** 存储冗余配置 */
case class StorageRedundancy(
  replicationFactor: Int = 3, // 副本数量
  erasureCoding: Option[ErasureCoding] = None,
  geographicRedundancy: Boolean = false, // 地理冗余
  backupEnabled: Boolean = true
)
