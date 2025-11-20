package app.mosia.nexus
package domain.model.common

/** 数学工具类 */
object MathUtils:
  // 数值限制
  def clamp(value: Double, min: Double, max: Double): Double =
    if value < min then min
    else if value > max then max
    else value

  def clamp01(value: Double): Double = clamp(value, 0.0, 1.0)
  def clamp11(value: Double): Double = clamp(value, -1.0, 1.0)

  // 角度弧度转换
  def degreesToRadians(degrees: Double): Double = degrees * math.Pi / 180.0
  def radiansToDegrees(radians: Double): Double = radians * 180.0 / math.Pi

  // 线性插值
  def lerp(a: Double, b: Double, t: Double): Double =
    a + (b - a) * clamp01(t)

  // 平滑插值
  def smoothstep(edge0: Double, edge1: Double, x: Double): Double = {
    val t = clamp01((x - edge0) / (edge1 - edge0))
    t * t * (3.0 - 2.0 * t)
  }

  // 近似相等比较
  def approxEqual(a: Double, b: Double, tolerance: Double = 1e-6): Boolean =
    (a - b).abs < tolerance
