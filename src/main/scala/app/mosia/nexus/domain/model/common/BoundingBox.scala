package app.mosia.nexus.domain.model.common

// 边界框
case class BoundingBox(min: Position3D, max: Position3D) extends ValueObject:
  def center: Position3D = Position3D(
    (min.x + max.x) / 2,
    (min.y + max.y) / 2,
    (min.z + max.z) / 2
  )

  def size: Position3D = max - min

  def contains(point: Position3D): Boolean =
    point.isWithinBounds(min, max)

  def intersects(other: BoundingBox): Boolean =
    !(min.x > other.max.x || max.x < other.min.x ||
      min.y > other.max.y || max.y < other.min.y ||
      min.z > other.max.z || max.z < other.min.z)

  def expanded(margin: Double): BoundingBox =
    BoundingBox(min - Position3D(margin, margin, margin), max + Position3D(margin, margin, margin))

object BoundingBox:
  def fromPoints(points: List[Position3D]): Option[BoundingBox] =
    if (points.isEmpty) None
    else
      Some(
        BoundingBox(
          Position3D(
            points.map(_.x).min,
            points.map(_.y).min,
            points.map(_.z).min
          ),
          Position3D(
            points.map(_.x).max,
            points.map(_.y).max,
            points.map(_.z).max
          )
        )
      )
