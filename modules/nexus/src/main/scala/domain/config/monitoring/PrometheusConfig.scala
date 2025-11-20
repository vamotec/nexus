package app.mosia.nexus
package domain.config.monitoring

/** Prometheus 配置 */
case class PrometheusConfig(
  enabled: Boolean,
  port: Int,
  path: String
)
