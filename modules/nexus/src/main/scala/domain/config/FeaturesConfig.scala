package app.mosia.nexus
package domain.config

/** 功能开关配置 */
case class FeaturesConfig(
  graphqlSubscriptions: Boolean,
  websocketControl: Boolean,
  eventPublishing: Boolean,
  metricsExport: Boolean
)
