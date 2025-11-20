package app.mosia.nexus
package domain.model.task

/** 评估指标 */
case class EvaluationMetric(
  name: String,
//                             metricType: MetricType,
  weight: Double,
  targetValue: Option[Double],
  acceptableRange: Option[Range]
)
