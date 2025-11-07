package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.common.Bandwidth
import zio.Duration

object SLA:
  /** 服务等级协议等级 */
  enum ServiceLevelAgreement:
    case Standard, Premium, Enterprise, Critical

  /** 详细的SLA配置 */
  case class SLAConfiguration(
    level: ServiceLevelAgreement,

    // 可用性保证
    availability: AvailabilitySLA,

    // 性能保证
    performance: PerformanceSLA,

    // 支持保证
    support: SupportSLA,

    // 恢复目标
    recovery: RecoverySLA,

    // 安全合规
//                               security: SecuritySLA,

    // 成本限制
    cost: CostSLA
  )

  /** 可用性SLA */
  case class AvailabilitySLA(
    uptime: Double, // 正常运行时间比例，如 0.9995
    scheduledMaintenance: MaintenanceWindow,
    maxConsecutiveDowntime: Duration,
    notificationPeriod: Duration = Duration.fromSeconds(24 * 60 * 60)
  )

  /** 性能SLA */
  case class PerformanceSLA(
    responseTime: Duration, // 最大响应时间
    throughput: Bandwidth, // 最小吞吐量
    resourceGuarantee: Double = 1.0, // 资源保证比例
    degradationThreshold: Double = 0.8 // 性能降级阈值
  )

  /** 支持SLA */
  case class SupportSLA(
    responseTime: Map[Severity, Duration], // 按严重程度的响应时间
    resolutionTime: Map[Severity, Duration], // 解决时间
    supportChannels: Set[SupportChannel], // 支持渠道
    businessHours: BusinessHours // 支持时间
  )

  /** 恢复SLA */
  case class RecoverySLA(
    rto: Duration, // 恢复时间目标
    rpo: Duration, // 恢复点目标
    backupFrequency: Duration, // 备份频率
    dataRetention: Duration = Duration.fromSeconds(30 * 60 * 60 * 24) // 数据保留期
  )

  /** 安全SLA */
//  case class SecuritySLA(
//                          encryption: EncryptionRequirements,
//                          compliance: Set[ComplianceStandard],
//                          audit: AuditRequirements,
//                          accessControl: AccessControlRequirements
//                        )

  /** 成本SLA */
  case class CostSLA(
    maxCostPerHour: BigDecimal,
//                      budgetAlerts: List[BudgetAlert],
//                      optimization: CostOptimization
  )

  /** 维护窗口 */
  case class MaintenanceWindow(
    frequency: Duration = Duration.fromSeconds(30 * 60 * 60 * 24),
    duration: Duration = Duration.fromSeconds(4 * 60 * 60),
    advanceNotice: Duration = Duration.fromSeconds(7 * 60 * 60 * 24),
    allowedTimes: List[TimeRange]
  )

  /** 支持渠道 */
  enum SupportChannel:
    case Email, Phone, Chat, Ticket, OnSite

  /** 问题严重程度 */
  enum Severity:
    case Critical, High, Medium, Low

  /** 业务时间 */
  case class BusinessHours(
    timezone: String = "UTC",
    weekdays: TimeRange = TimeRange(9, 17), // 工作日 9-17点
    weekends: Option[TimeRange] = None, // 周末支持
    holidays: Boolean = false // 节假日支持
  )

  case class TimeRange(start: Int, end: Int) // 小时数，如 9-17
