package app.mosia.nexus
package domain.config.monitoring

/** 监控配置 */
case class MonitoringConfig(
  prometheus: PrometheusConfig,
  healthCheck: HealthCheckConfig,
  metrics: MetricsConfig
)
