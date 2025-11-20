package app.mosia.nexus
package domain.model.network

/** 端口范围 */
case class PortRange(
  start: Int,
  end: Int
):
  require(start >= 0 && start <= 65535, "Port must be between 0 and 65535")
  require(end >= start && end <= 65535, "End port must be >= start port")

  def contains(port: Int): Boolean = port >= start && port <= end
  def size: Int                    = end - start + 1
