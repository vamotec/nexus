package app.mosia.nexus
package domain.config.jobs

/** 后台任务配置 */
case class JobsConfig(
  sessionMetricsSync: JobConfig,
  trainingProgressSync: JobConfig,
  quotaUsageCalculation: CronJobConfig,
  expiredSessionCleanup: CleanupJobConfig
)
