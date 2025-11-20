package app.mosia.nexus
package domain.model.storage

/** 文件系统类型 */
enum FileSystemType:
  case Ext4, XFS, NTFS, FAT32, ZFS, Btrfs, NFS, CIFS
