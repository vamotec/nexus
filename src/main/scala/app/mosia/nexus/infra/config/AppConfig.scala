package app.mosia.nexus.infra.config

case class AppConfig(
  http: HttpConfig,
  graphql: GraphqlConfig,
  db: DbSource,
  neuro: NeuroConfig,
  auth: AuthConfig,
  kafka: KafkaConfig,
  quota: QuotaConfig,
  monitoring: MonitoringConfig,
  logging: LoggingConfig,
  jobs: JobsConfig,
  features: FeaturesConfig,
  limit: LimitConfig,
  cors: NexusCorsConfig,
  device: DeviceServiceConfig
)
