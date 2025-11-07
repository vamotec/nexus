package app.mosia.nexus.domain.model.storage

/** 存储挂载点配置 */
case class MountPointConfig(
  path: String, // 挂载路径，如 "/data", "/logs"
  device: Option[String] = None, // 设备名，如 "/dev/sdb1"
  fileSystem: FileSystemType = FileSystemType.Ext4,
  options: Map[String, String] = Map.empty, // 挂载选项
  readOnly: Boolean = false,
  encrypted: Boolean = false,
  backupPolicy: BackupPolicy = BackupPolicy.None
)
