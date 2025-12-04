package app.mosia.nexus
package domain.model.scene

import zio.json.*

enum ObstacleType derives JsonCodec:
  case Box, Sphere, Cylinder, Mesh

object ObstacleType:
  def fromString(s: String): ObstacleType = s.toLowerCase match
    case "box" => ObstacleType.Box
    case "sphere" => ObstacleType.Sphere
    case "cylinder" => ObstacleType.Cylinder
    case "mesh" => ObstacleType.Mesh
    case _ => throw new IllegalArgumentException(s"Unknown obstacle type: $s")

  def toString(obstacleType: ObstacleType): String = obstacleType match
    case ObstacleType.Box => "box"
    case ObstacleType.Sphere => "sphere"
    case ObstacleType.Cylinder => "cylinder"
    case ObstacleType.Mesh => "mesh"
