package app.mosia.nexus
package application.dto.response.common

case class FpsStats(
  avg: Double,
  max: Double,
  min: Double,
  p50: Option[Double] = None,
  p99: Option[Double] = None
)
