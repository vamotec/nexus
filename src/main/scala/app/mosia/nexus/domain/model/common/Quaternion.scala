package app.mosia.nexus.domain.model.common

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.JsonCodec

/** 四元数 - 用于表示旋转 */
case class Quaternion(x: Double, y: Double, z: Double, w: Double) extends ValueObject
    derives JsonCodec,
      Schema,
      Cs.SemiAuto,
      ArgBuilder:
  def normalized: Quaternion =
    val mag = math.sqrt(x * x + y * y + z * z + w * w)
    if (mag > 0) Quaternion(x / mag, y / mag, z / mag, w / mag)
    else Quaternion.identity

  def toEulerAngles: Vector3 =
    // 四元数转欧拉角实现
    // 简化实现，实际需要完整数学转换
    Vector3(
      math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y)),
      math.asin(2 * (w * y - z * x)),
      math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z))
    )

object Quaternion:
  val identity: Quaternion = Quaternion(0, 0, 0, 1)

  def fromEulerAngles(pitch: Double, yaw: Double, roll: Double): Quaternion =
    // 欧拉角转四元数实现
    val cy = math.cos(yaw * 0.5)
    val sy = math.sin(yaw * 0.5)
    val cp = math.cos(pitch * 0.5)
    val sp = math.sin(pitch * 0.5)
    val cr = math.cos(roll * 0.5)
    val sr = math.sin(roll * 0.5)

    Quaternion(
      cy * cp * sr - sy * sp * cr,
      sy * cp * sr + cy * sp * cr,
      sy * cp * cr - cy * sp * sr,
      cy * cp * cr + sy * sp * sr
    )
