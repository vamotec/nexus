package app.mosia.nexus.domain.model.common

// 球坐标
case class SphericalCoordinates(radius: Double, theta: Double, phi: Double) extends ValueObject:
  def toPosition3D: Position3D = Position3D.fromSpherical(this)
