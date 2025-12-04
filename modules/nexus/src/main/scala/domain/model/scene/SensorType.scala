package app.mosia.nexus
package domain.model.scene

import zio.json.*
import zio.*

enum SensorType derives JsonCodec:
  case Camera, Lidar, IMU, ForceTorque, DepthCamera

object SensorType:
  def fromString(s: String): SensorType = s.toLowerCase match
    case "camera" => SensorType.Camera
    case "lidar" => SensorType.Lidar
    case "imu" => SensorType.IMU
    case "force_torque" => SensorType.ForceTorque
    case "depth_camera" => SensorType.DepthCamera
    case _ => throw new IllegalArgumentException(s"Unknown obstacle type: $s")

  def toString(sensorType: SensorType): String = sensorType match
    case SensorType.Camera => "camera"
    case SensorType.Lidar => "lidar"
    case SensorType.IMU => "imu"
    case SensorType.ForceTorque => "force_torque"
    case SensorType.DepthCamera => "depth_camera"
