package app.mosia.nexus
package application.dto.response.common

case class GpuStats(
  avgUtilization: Double,
  maxUtilization: Option[Double] = None,
  maxMemoryMb: Long
)
