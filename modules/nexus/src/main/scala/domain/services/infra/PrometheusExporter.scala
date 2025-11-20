package app.mosia.nexus
package domain.services.infra

import zio.json.*
import zio.*

/** Prometheus 指标导出器
  *
  * 将 ZIO Metrics 收集的指标导出为 Prometheus 格式
  */
trait PrometheusExporter:
  def getMetrics: UIO[String]
