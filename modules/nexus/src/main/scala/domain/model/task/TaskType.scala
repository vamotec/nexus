package app.mosia.nexus
package domain.model.task

/** 任务类型 */
enum TaskType:
  case Navigation, ObjectDetection, ObstacleAvoidance, LaneFollowing,
    Parking, TrafficLightCompliance, PedestrianInteraction,
    DataCollection, Training, Evaluation
