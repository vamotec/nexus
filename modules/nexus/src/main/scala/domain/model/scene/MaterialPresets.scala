package app.mosia.nexus
package domain.model.scene

import domain.model.common.Color

object MaterialPresets:
  val Wood = Material(
    name = "wood",
    color = Some(Color(0.6, 0.4, 0.2)),
    texture = Some("wood_texture.png"),
    friction = 0.4,
    restitution = 0.3
  )

  val Metal = Material(
    name = "metal",
    color = Some(Color(0.8, 0.8, 0.8)),
    texture = Some("metal_texture.png"),
    friction = 0.2,
    restitution = 0.5
  )

  val Plastic = Material(
    name = "plastic",
    color = Some(Color(0.9, 0.9, 0.9)),
    texture = None,
    friction = 0.3,
    restitution = 0.7
  )

  val Rubber = Material(
    name = "rubber",
    color = Some(Color(0.0, 0.1, 0.0)),
    texture = None,
    friction = 0.5,
    restitution = 0.9
  )

  val Glass = Material(
    name = "glass",
    color = Some(Color(1.0, 1.0, 1.0, 0.1)),
    texture = None,
    friction = 0.1,
    restitution = 0.5
  )

  def fromType(materialType: MaterialType): Material =
    materialType match
      case MaterialType.Wood => Wood
      case MaterialType.Metal => Metal
      case MaterialType.Plastic => Plastic
      case MaterialType.Rubber => Rubber
      case MaterialType.Glass => Glass
      case MaterialType.Custom =>
        Material("custom", None, None, 0.5, 0.5) // 默认值
