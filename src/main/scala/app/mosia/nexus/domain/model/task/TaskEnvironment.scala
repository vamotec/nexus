package app.mosia.nexus.domain.model.task

import app.mosia.nexus.domain.model.common.Condition.*

/** 任务环境 */
case class TaskEnvironment(
  weather: WeatherCondition,
  trafficDensity: TrafficDensity,
  pedestrianDensity: PedestrianDensity,
  timeOfDay: TimeOfDay,
  roadConditions: RoadCondition,
  specialEvents: List[SpecialEvent]
)
