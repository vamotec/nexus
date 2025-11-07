package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.resource.SLA.ServiceLevelAgreement
import zio.Duration

/** 服务质量 */
case class QualityOfService(
  availability: Double = 0.99, // 可用性要求
  maxRecoveryTime: Duration = Duration.fromSeconds(300),
  backupRequired: Boolean = false,
  disasterRecovery: Boolean = false,
  sla: ServiceLevelAgreement = ServiceLevelAgreement.Standard
)
