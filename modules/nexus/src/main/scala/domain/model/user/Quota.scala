package app.mosia.nexus
package domain.model.user

import java.time.Instant
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

/** 用户配额
  *
  * @param maxConcurrentSessions 最大并发会话数
  * @param maxGpuHoursPerMonth 每月最大 GPU 小时数
  * @param maxStorageGb 最大存储空间 GB (免费用户: 5GB USD 存储)
  * @param maxOwnedOrganizations 最大拥有的组织数 (免费用户: 2)
  * @param maxJoinedOrganizations 最大加入的组织数 (免费用户: 无限制)
  * @param currentActiveSessions 当前活跃会话数
  * @param currentGpuHoursThisMonth 本月已使用 GPU 小时数
  * @param currentStorageGb 当前存储空间使用量 GB
  * @param currentOwnedOrganizations 当前拥有的组织数
  * @param quotaResetAt 配额重置时间
  */
case class Quota(
  maxConcurrentSessions: Int,
  maxGpuHoursPerMonth: Double,
  maxStorageGb: Double,
  maxOwnedOrganizations: Int,
  maxJoinedOrganizations: Int,
  currentActiveSessions: Int,
  currentGpuHoursThisMonth: Double,
  currentStorageGb: Double,
  currentOwnedOrganizations: Int,
  quotaResetAt: Instant
) derives JsonCodec:
  def hasAvailableSessions: Boolean = currentActiveSessions < maxConcurrentSessions

  def remainingGpuHours: Double = maxGpuHoursPerMonth - currentGpuHoursThisMonth

  /** 检查是否可以创建新组织 */
  def canCreateOrganization: Boolean = currentOwnedOrganizations < maxOwnedOrganizations

  /** 剩余可创建组织数 */
  def remainingOrganizations: Int = Math.max(0, maxOwnedOrganizations - currentOwnedOrganizations)

  /** 检查存储空间是否充足 */
  def hasStorageAvailable(requiredGb: Double): Boolean =
    currentStorageGb + requiredGb <= maxStorageGb

  /** 剩余存储空间 */
  def remainingStorageGb: Double = Math.max(0, maxStorageGb - currentStorageGb)

  /** 增加拥有的组织数 */
  def incrementOwnedOrganizations: Quota =
    copy(currentOwnedOrganizations = currentOwnedOrganizations + 1)

  /** 减少拥有的组织数 */
  def decrementOwnedOrganizations: Quota =
    copy(currentOwnedOrganizations = Math.max(0, currentOwnedOrganizations - 1))

object Quota:
  /** 免费用户默认配额 */
  val free: Quota = Quota(
    maxConcurrentSessions = 3,
    maxGpuHoursPerMonth = 100.0,
    maxStorageGb = 5.0, // 5GB USD 存储
    maxOwnedOrganizations = 2, // 最多拥有 2 个组织
    maxJoinedOrganizations = Int.MaxValue, // 无限制加入组织
    currentActiveSessions = 0,
    currentGpuHoursThisMonth = 0.0,
    currentStorageGb = 0.0,
    currentOwnedOrganizations = 0,
    quotaResetAt = Instant.now().plusSeconds(30 * 24 * 3600) // 30 天后重置
  )

  /** 付费用户配额 */
  val premium: Quota = Quota(
    maxConcurrentSessions = 10,
    maxGpuHoursPerMonth = 1000.0,
    maxStorageGb = 100.0,
    maxOwnedOrganizations = 10,
    maxJoinedOrganizations = Int.MaxValue,
    currentActiveSessions = 0,
    currentGpuHoursThisMonth = 0.0,
    currentStorageGb = 0.0,
    currentOwnedOrganizations = 0,
    quotaResetAt = Instant.now().plusSeconds(30 * 24 * 3600)
  )
