package app.mosia.nexus
package domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 三维向量 - 用于表示位置、方向、速度等 */
case class Vector3D(
  x: Double,
  y: Double,
  z: Double
) derives JsonCodec,
      Cs.SemiAuto:
  // 向量运算
  def +(other: Vector3D): Vector3D =
    Vector3D(x + other.x, y + other.y, z + other.z)

  def -(other: Vector3D): Vector3D =
    Vector3D(x - other.x, y - other.y, z - other.z)

  def *(scalar: Double): Vector3D =
    Vector3D(x * scalar, y * scalar, z * scalar)

  def /(scalar: Double): Vector3D =
    Vector3D(x / scalar, y / scalar, z / scalar)

  // 点积
  def dot(other: Vector3D): Double =
    x * other.x + y * other.y + z * other.z

  // 叉积
  def cross(other: Vector3D): Vector3D =
    Vector3D(
      y * other.z - z * other.y,
      z * other.x - x * other.z,
      x * other.y - y * other.x
    )

  // 向量长度
  def magnitude: Double =
    math.sqrt(x * x + y * y + z * z)

  // 单位向量
  def normalized: Vector3D = {
    val mag = magnitude
    if (mag > 0) this / mag else Vector3D.zero
  }

  // 距离计算
  def distanceTo(other: Vector3D): Double =
    (this - other).magnitude

  // 线性插值
  def lerp(other: Vector3D, t: Double): Vector3D =
    this + (other - this) * t

  // 角度计算
  def angleTo(other: Vector3D): Double = {
    val dotProduct = this.normalized.dot(other.normalized)
    math.acos(MathUtils.clamp11(dotProduct))
  }

  // 带容差的角度计算
  def angleTo(other: Vector3D, tolerance: Double): Double = {
    val dot     = this.normalized.dot(other.normalized)
    val clamped = MathUtils.clamp(dot, -1.0 + tolerance, 1.0 - tolerance)
    math.acos(clamped)
  }

  /** 计算与另一个向量的夹角（角度） */
  def angleToDegrees(other: Vector3D): Double =
    MathUtils.radiansToDegrees(angleTo(other))

  /** 带数值稳定性的角度计算 */
  def safeAngleTo(other: Vector3D, epsilon: Double = 1e-10): Double = {
    val dot = this.normalized.dot(other.normalized)
    // 处理数值精度问题
    if dot >= 1.0 - epsilon then 0.0
    else if dot <= -1.0 + epsilon then math.Pi
    else math.acos(dot)
  }

  /** 计算带符号的角度（需要参考平面） */
  def signedAngleTo(other: Vector3D, axis: Vector3D): Double = {
    val angle = angleTo(other)
    val cross = this.cross(other)
    val sign  = if axis.dot(cross) < 0 then -1.0 else 1.0
    angle * sign
  }

  /** 检查是否与另一个向量平行 */
  def isParallelTo(other: Vector3D, tolerance: Double = 1e-6): Boolean = {
    val angle = angleTo(other)
    angle < tolerance || (math.Pi - angle) < tolerance
  }

  /** 检查是否与另一个向量垂直 */
  def isPerpendicularTo(other: Vector3D, tolerance: Double = 1e-6): Boolean = {
    val dot = this.normalized.dot(other.normalized)
    MathUtils.approxEqual(dot.abs, 0.0, tolerance)
  }

  // 坐标转换
  def toArray: Array[Double] = Array(x, y, z)

  override def toString: String = f"Vector3($x%.2f, $y%.2f, $z%.2f)"

object Vector3D:
  // 常用常量
  val zero: Vector3D    = Vector3D(0, 0, 0)
  val one: Vector3D     = Vector3D(1, 1, 1)
  val up: Vector3D      = Vector3D(0, 1, 0)
  val down: Vector3D    = Vector3D(0, -1, 0)
  val forward: Vector3D = Vector3D(0, 0, 1)
  val back: Vector3D    = Vector3D(0, 0, -1)
  val right: Vector3D   = Vector3D(1, 0, 0)
  val left: Vector3D    = Vector3D(-1, 0, 0)

  // 工厂方法
  def apply(array: Array[Double]): Vector3D =
    if (array.length >= 3) Vector3D(array(0), array(1), array(2))
    else throw new IllegalArgumentException("Array must have at least 3 elements")

  def fromSpherical(radius: Double, theta: Double, phi: Double): Vector3D =
    Vector3D(
      radius * math.sin(phi) * math.cos(theta),
      radius * math.cos(phi),
      radius * math.sin(phi) * math.sin(theta)
    )

  // 随机向量
  def random(scale: Double = 1.0): Vector3D =
    Vector3D(
      (math.random() - 0.5) * 2 * scale,
      (math.random() - 0.5) * 2 * scale,
      (math.random() - 0.5) * 2 * scale
    )
