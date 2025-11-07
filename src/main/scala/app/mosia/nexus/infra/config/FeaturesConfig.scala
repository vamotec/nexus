package app.mosia.nexus.infra.config

case class FeaturesConfig(
  playgroundEnalble: Boolean,
  graphqlSubscriptions: Boolean,
  websocketControl: Boolean,
  eventPublishing: Boolean,
  metricsExport: Boolean
)
