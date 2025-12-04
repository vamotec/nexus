package app.mosia.nexus
package domain.model.scene

import domain.error.*
import domain.model.common.ValueObject

import zio.json.*
import zio.*

enum RobotType derives JsonCodec:
  case FrankaPanda, UR5, Kuka
  case Custom(name: String)

object RobotType:
  def fromString(str: String): Either[String, RobotType] = str.toLowerCase match
    case "franka_panda" => Right(RobotType.FrankaPanda)
    case "ur5" => Right(RobotType.UR5)
    case "kuka" => Right(RobotType.Kuka)
    case other if other.startsWith("custom:") =>
      val name = other.stripPrefix("custom:")
      if name.nonEmpty then Right(RobotType.Custom(name))
      else Left(s"Invalid custom robot name: $str")
    case _ => Left(s"Invalid robot type: $str")

  def toRoString(robotType: RobotType): String = robotType match
    case RobotType.FrankaPanda => "franka_panda"
    case RobotType.UR5 => "ur5"
    case RobotType.Kuka => "kuka"
    case RobotType.Custom(name) => s"custom:$name"
