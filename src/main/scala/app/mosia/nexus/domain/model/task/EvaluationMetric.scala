package app.mosia.nexus.domain.model.task

/** 评估指标 */
case class EvaluationMetric(
  name: String,
//                             metricType: MetricType,
  weight: Double,
  targetValue: Option[Double],
  acceptableRange: Option[Range]
)
