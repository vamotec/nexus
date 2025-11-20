package app.mosia.nexus
package domain.model.common

/** 支持的数据类型 */
case class MemorySize(bytes: Long):
  def toGB: Double = bytes / (1024 * 1024 * 1024).toDouble
  def toMB: Double = bytes / (1024 * 1024).toDouble

object MemorySize:
  def fromGB(gb: Double): MemorySize = MemorySize((gb * 1024 * 1024 * 1024).toLong)
  def fromMB(mb: Double): MemorySize = MemorySize((mb * 1024 * 1024).toLong)
