package app.mosia.nexus.domain.model.task

import app.mosia.nexus.domain.model.common.Vector3

enum ObjectiveTarget:
  case ReachPosition(position: Vector3, tolerance: Double)
  case FollowPath(waypoints: List[Vector3], maxDeviation: Double)
  case AvoidCollision
  case MaintainSpeed(targetSpeed: Double, tolerance: Double)
  case DetectObjects(objectTypes: Set[String], minConfidence: Double)
  case CompleteManeuver(maneuverType: String)
  case Custom(condition: String)
