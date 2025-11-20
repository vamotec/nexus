package app.mosia.nexus
package domain.model.task

import domain.model.common.Vector3D

enum ObjectiveTarget:
  case ReachPosition(position: Vector3D, tolerance: Double)
  case FollowPath(waypoints: List[Vector3D], maxDeviation: Double)
  case AvoidCollision
  case MaintainSpeed(targetSpeed: Double, tolerance: Double)
  case DetectObjects(objectTypes: Set[String], minConfidence: Double)
  case CompleteManeuver(maneuverType: String)
  case Custom(condition: String)
