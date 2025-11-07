package app.mosia.nexus.domain.model.common

// 柱坐标
case class CylindricalCoordinates(radius: Double, theta: Double, height: Double) extends ValueObject:
  def toPosition3D: Position3D = Position3D.fromCylindrical(this)
