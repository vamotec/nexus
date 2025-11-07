package app.mosia.nexus.domain.model.scene

import app.mosia.nexus.domain.model.common.{Color, ValueObject}

case class Material(name: String, color: Option[Color], texture: Option[String], friction: Double, restitution: Double)
    extends ValueObject
