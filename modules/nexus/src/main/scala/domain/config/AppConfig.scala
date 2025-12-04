package app.mosia.nexus
package domain.config

import domain.config.auth.AuthConfig
import domain.config.cache.CacheConfig
import domain.config.database.DbConfig
import domain.config.jobs.JobsConfig
import domain.config.limit.LimitConfig
import domain.config.monitoring.MonitoringConfig
import domain.config.cloud.CloudConfig
import domain.config.quota.QuotaConfig
import domain.config.messaging.RabbitMQConfig
import domain.config.notification.NotificationConfig

/** 应用配置 - 根配置类 */
case class AppConfig(
  http: HttpConfig,
  db: DbConfig,
  cloud: CloudConfig,
  auth: AuthConfig,
  quota: QuotaConfig,
  monitoring: MonitoringConfig,
  jobs: JobsConfig,
  features: FeaturesConfig,
  limit: LimitConfig,
  cors: CorsConfig,
  device: DeviceConfig,
  cache: CacheConfig,
  rabbitmq: RabbitMQConfig,
  notification: NotificationConfig
)
