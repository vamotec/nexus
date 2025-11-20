package app.mosia.nexus
package domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 三维位置 - 用于仿真中的精确坐标表示 */
case class Position3D(x: Double, y: Double, z: Double) extends ValueObject
    derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder:
  // 算术运算
  def +(other: Position3D): Position3D =
    Position3D(x + other.x, y + other.y, z + other.z)

  def -(other: Position3D): Position3D =
    Position3D(x - other.x, y - other.y, z - other.z)

  def *(scalar: Double): Position3D =
    Position3D(x * scalar, y * scalar, z * scalar)

  def /(scalar: Double): Position3D =
    if (scalar != 0) Position3D(x / scalar, y / scalar, z / scalar)
    else throw new IllegalArgumentException("Division by zero")

  // 向量运算
  def dot(other: Position3D): Double =
    x * other.x + y * other.y + z * other.z

  def cross(other: Position3D): Position3D =
    Position3D(
      y * other.z - z * other.y,
      z * other.x - x * other.z,
      x * other.y - y * other.x
    )

  // 距离和长度
  def distance(other: Position3D): Double =
    math.sqrt(
      math.pow(x - other.x, 2) +
        math.pow(y - other.y, 2) +
        math.pow(z - other.z, 2)
    )

  def magnitude: Double =
    math.sqrt(x * x + y * y + z * z)

  def normalized: Position3D = {
    val mag = magnitude
    if (mag > 0) this / mag else Position3D.zero
  }

  // 几何关系
  def isWithinRadius(center: Position3D, radius: Double): Boolean =
    distance(center) <= radius

  def isWithinBounds(min: Position3D, max: Position3D): Boolean =
    x >= min.x && x <= max.x &&
      y >= min.y && y <= max.y &&
      z >= min.z && z <= max.z

  // 插值和变换
  def lerp(target: Position3D, t: Double): Position3D =
    this + (target - this) * t

  def rotate(axis: Position3D, angle: Double): Position3D = {
    // 绕轴旋转 (简化实现)
    val cos = math.cos(angle)
    val sin = math.sin(angle)
    val dot = this.dot(axis)

    this * cos + axis.cross(this) * sin + axis * (dot * (1 - cos))
  }

  // 坐标转换
  def toSpherical: SphericalCoordinates = {
    val radius = magnitude
    val theta  = if (radius > 0) math.acos(z / radius) else 0.0
    val phi    = if (x != 0 || y != 0) math.atan2(y, x) else 0.0
    SphericalCoordinates(radius, theta, phi)
  }

  def toCylindrical: CylindricalCoordinates = {
    val radius = math.sqrt(x * x + y * y)
    val theta  = if (x != 0 || y != 0) math.atan2(y, x) else 0.0
    CylindricalCoordinates(radius, theta, z)
  }

  // 投影
  def projectToPlane(normal: Position3D): Position3D = {
    val n = normal.normalized
    this - n * (this.dot(n))
  }

  // 比较和验证
  def approxEquals(other: Position3D, tolerance: Double = 1e-6): Boolean =
    (x - other.x).abs < tolerance &&
      (y - other.y).abs < tolerance &&
      (z - other.z).abs < tolerance

  def isValid: Boolean =
    !x.isNaN && !y.isNaN && !z.isNaN &&
      !x.isInfinite && !y.isInfinite && !z.isInfinite

  // 格式化和输出
  def toArray: Array[Double] = Array(x, y, z)

  def toMap: Map[String, Double] =
    Map("x" -> x, "y" -> y, "z" -> z)

//  override def toString: String =
//    f"Position3D(x=%.3f, y=%.3f, z=%.3f)".format(x, y, z)

object Position3D:
  // 常用常量
  val zero: Position3D = Position3D(0, 0, 0)
  val one: Position3D  = Position3D(1, 1, 1)

  // 坐标轴单位向量
  val unitX: Position3D = Position3D(1, 0, 0)
  val unitY: Position3D = Position3D(0, 1, 0)
  val unitZ: Position3D = Position3D(0, 0, 1)

  // 工厂方法
  def apply(array: Array[Double]): Position3D =
    if (array.length >= 3) Position3D(array(0), array(1), array(2))
    else throw new IllegalArgumentException("Array must have at least 3 elements")

  def fromMap(map: Map[String, Double]): Position3D =
    Position3D(
      map.getOrElse("x", 0.0),
      map.getOrElse("y", 0.0),
      map.getOrElse("z", 0.0)
    )

  def fromSpherical(spherical: SphericalCoordinates): Position3D =
    Position3D(
      spherical.radius * math.sin(spherical.theta) * math.cos(spherical.phi),
      spherical.radius * math.sin(spherical.theta) * math.sin(spherical.phi),
      spherical.radius * math.cos(spherical.theta)
    )

  def fromCylindrical(cylindrical: CylindricalCoordinates): Position3D =
    Position3D(
      cylindrical.radius * math.cos(cylindrical.theta),
      cylindrical.radius * math.sin(cylindrical.theta),
      cylindrical.height
    )

  // 随机位置生成
  def randomInSphere(center: Position3D, radius: Double): Position3D = {
    val theta = 2 * math.Pi * math.random()
    val phi   = math.acos(2 * math.random() - 1)
    val r     = radius * math.cbrt(math.random())

    center + Position3D(
      r * math.sin(phi) * math.cos(theta),
      r * math.sin(phi) * math.sin(theta),
      r * math.cos(phi)
    )
  }

  def randomInCube(center: Position3D, sideLength: Double): Position3D = {
    val halfSide = sideLength / 2
    center + Position3D(
      (math.random() - 0.5) * sideLength,
      (math.random() - 0.5) * sideLength,
      (math.random() - 0.5) * sideLength
    )
  }

  // 路径生成
  def linearPath(start: Position3D, end: Position3D, numPoints: Int): List[Position3D] =
    (0 until numPoints).map { i =>
      val t = i.toDouble / (numPoints - 1)
      start.lerp(end, t)
    }.toList
