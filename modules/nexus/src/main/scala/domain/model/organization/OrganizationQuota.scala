package app.mosia.nexus
package domain.model.organization

import zio.json.*

/** 组织配额
  *
  * @param maxUsers 最大用户数 (免费版: 5)
  * @param maxStorageGb 最大存储空间 GB (免费版: 100.0)
  * @param maxGpuHoursPerMonth 每月最大 GPU 小时数 (免费版: 500.0)
  */
case class OrganizationQuota(
  maxUsers: Int,
  maxStorageGb: Double,
  maxGpuHoursPerMonth: Double
) derives JsonCodec:

  /** 检查是否可以添加新成员 */
  def canAddMember(currentMemberCount: Int): Boolean =
    currentMemberCount < maxUsers

  /** 检查存储空间是否充足 */
  def hasStorageAvailable(currentStorageGb: Double, requiredGb: Double): Boolean =
    currentStorageGb + requiredGb <= maxStorageGb

  /** 剩余存储空间 */
  def remainingStorageGb(currentStorageGb: Double): Double =
    Math.max(0, maxStorageGb - currentStorageGb)

object OrganizationQuota:
  /** 免费版配额 */
  val free: OrganizationQuota = OrganizationQuota(
    maxUsers = 5,
    maxStorageGb = 100.0,
    maxGpuHoursPerMonth = 500.0
  )

  /** 高级版配额 */
  val premium: OrganizationQuota = OrganizationQuota(
    maxUsers = 50,
    maxStorageGb = 1000.0,
    maxGpuHoursPerMonth = 5000.0
  )

  /** 企业版配额 */
  val enterprise: OrganizationQuota = OrganizationQuota(
    maxUsers = Int.MaxValue,
    maxStorageGb = Double.MaxValue,
    maxGpuHoursPerMonth = Double.MaxValue
  )

  /** 根据计划类型获取配额 */
  def fromPlanType(planType: PlanType): OrganizationQuota = planType match
    case PlanType.Free => free
    case PlanType.Premium => premium
    case PlanType.Enterprise => enterprise
