package app.mosia.nexus
package domain.model.resource

import domain.model.resource.SLA.ServiceLevelAgreement

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 服务质量 */
case class QualityOfService(
  availability: Double = 0.99, // 可用性要求
  maxRecoveryTime: Duration = Duration.fromSeconds(300),
  backupRequired: Boolean = false,
  disasterRecovery: Boolean = false,
  sla: ServiceLevelAgreement = ServiceLevelAgreement.Standard
)
