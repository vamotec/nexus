package app.mosia.nexus.domain.model.storage

/** 存储类型分类 */
enum StorageType:
  // 基于介质的类型
  case HDD, SSD, NVMe, Optane

  // 基于性能的类型
  case Standard, Performance, Ultra

  // 基于架构的类型
  case Local, Network, Object, Block

  // 云存储类型
  case EBS, EFS, S3, GCS, AzureBlob
