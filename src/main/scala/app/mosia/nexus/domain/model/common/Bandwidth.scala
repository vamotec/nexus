package app.mosia.nexus.domain.model.common

case class Bandwidth(bps: Long):
  def toMbps: Double = bps / (1000 * 1000).toDouble
  def toGbps: Double = bps / (1000 * 1000 * 1000).toDouble

object Bandwidth:
  def fromMbps(mbps: Double): Bandwidth = Bandwidth((mbps * 1000 * 1000).toLong)
  def fromGbps(gbps: Double): Bandwidth = Bandwidth((gbps * 1000 * 1000 * 1000).toLong)
