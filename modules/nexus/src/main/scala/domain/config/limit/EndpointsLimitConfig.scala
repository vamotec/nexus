package app.mosia.nexus
package domain.config.limit

/** 端点限流配置 */
case class EndpointsLimitConfig(
  createSession: EndpointLimitConfig,
  startTraining: EndpointLimitConfig
)
