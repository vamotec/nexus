package app.mosia.nexus
package domain.config

import domain.config.auth.AuthConfig
import domain.config.cache.CacheConfig
import domain.config.database.DbSource
import domain.config.jobs.JobsConfig
import domain.config.kafka.KafkaConfig
import domain.config.limit.LimitConfig
import domain.config.monitoring.MonitoringConfig
import domain.config.neuro.NeuroConfig
import domain.config.quota.QuotaConfig

/** 应用配置 - 根配置类 */
case class AppConfig(
  http: HttpConfig,
  db: DbSource,
  neuro: NeuroConfig,
  auth: AuthConfig,
  kafka: KafkaConfig,
  quota: QuotaConfig,
  monitoring: MonitoringConfig,
  jobs: JobsConfig,
  features: FeaturesConfig,
  limit: LimitConfig,
  cors: CorsConfig,
  device: DeviceConfig,
  cache: CacheConfig
)
