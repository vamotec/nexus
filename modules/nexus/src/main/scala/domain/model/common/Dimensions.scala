package app.mosia.nexus
package domain.model.common

/** 三维尺寸 - 用于表示物体的长宽高 */
case class Dimensions(
  length: Double, // 长度 (X轴)
  width: Double, // 宽度 (Y轴)
  height: Double // 高度 (Z轴)
) {
  // 体积计算
  def volume: Double = length * width * height

  // 表面积计算 (简化，假设长方体)
  def surfaceArea: Double =
    2 * (length * width + length * height + width * height)

  // 最大尺寸
  def maxDimension: Double = length.max(width).max(height)

  // 最小尺寸
  def minDimension: Double = length.min(width).min(height)

  // 缩放
  def scale(factor: Double): Dimensions =
    Dimensions(length * factor, width * factor, height * factor)

  // 检查是否包含另一个尺寸
  def contains(other: Dimensions): Boolean =
    length >= other.length && width >= other.width && height >= other.height

  // 边界框
  def boundingBox: (Vector3D, Vector3D) =
    (Vector3D(-length / 2, -width / 2, -height / 2), Vector3D(length / 2, width / 2, height / 2))

//  override def toString: String =
//    f"Dimensions(length=%.2f, width=%.2f, height=%.2f)".format(length, width, height)
}

object Dimensions {
  // 常用尺寸
  val zero: Dimensions = Dimensions(0, 0, 0)
  val one: Dimensions  = Dimensions(1, 1, 1)

  // 工厂方法
  def cube(side: Double): Dimensions = Dimensions(side, side, side)

  def fromArray(array: Array[Double]): Dimensions =
    if (array.length >= 3) Dimensions(array(0), array(1), array(2))
    else throw new IllegalArgumentException("Array must have at least 3 elements")

  // 车辆标准尺寸
  def compactCar: Dimensions = Dimensions(4.2, 1.8, 1.5)
  def sedan: Dimensions      = Dimensions(4.8, 1.9, 1.5)
  def suv: Dimensions        = Dimensions(5.0, 2.0, 1.8)
  def truck: Dimensions      = Dimensions(6.5, 2.5, 2.2)

  // 行人尺寸
  def adult: Dimensions = Dimensions(0.5, 0.5, 1.7)
  def child: Dimensions = Dimensions(0.4, 0.4, 1.2)
}
