package app.mosia.nexus
package domain.services.infra

import domain.error.*

/** 系统指标收集器
  *
  * 收集 JVM、系统和业务指标，导出到 Prometheus
  */
trait SystemMetricsCollector:
  def startCollection(): AppTask[Unit]
  def collectOnce(): AppTask[Unit]
