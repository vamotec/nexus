package app.mosia.nexus
package domain.model.organization

import java.time.Instant
import sttp.tapir.Schema
import zio.json.*

/** 组织
  *
  * @param id 组织 ID
  * @param name 组织名称
  * @param description 组织描述
  * @param avatar 组织头像 URL
  * @param planType 计划类型 (Free, Premium, Enterprise)
  * @param quota 组织配额
  * @param isActive 是否激活
  * @param isDeleted 是否已删除
  * @param createdAt 创建时间
  * @param updatedAt 更新时间
  * @param deletedAt 删除时间
  */
case class Organization(
  id: OrganizationId,
  name: String,
  description: Option[String],
  avatar: Option[String],
  planType: PlanType,
  quota: OrganizationQuota,
  isActive: Boolean,
  isDeleted: Boolean,
  createdAt: Instant,
  updatedAt: Instant,
  deletedAt: Option[Instant]
) derives JsonCodec:

  /** 检查组织是否可用 */
  def isAvailable: Boolean = isActive && !isDeleted

  /** 检查是否可以添加新成员 */
  def canAddMember(currentMemberCount: Int): Boolean =
    isAvailable && quota.canAddMember(currentMemberCount)

  /** 检查是否为免费计划 */
  def isFree: Boolean = planType == PlanType.Free

  /** 检查是否为付费计划 */
  def isPaid: Boolean = planType != PlanType.Free

  /** 更新为高级版 */
  def upgradeToPremium: Organization =
    copy(
      planType = PlanType.Premium,
      quota = OrganizationQuota.premium,
      updatedAt = Instant.now()
    )

  /** 更新为企业版 */
  def upgradeToEnterprise: Organization =
    copy(
      planType = PlanType.Enterprise,
      quota = OrganizationQuota.enterprise,
      updatedAt = Instant.now()
    )

  /** 软删除 */
  def softDelete: Organization =
    copy(
      isDeleted = true,
      isActive = false,
      deletedAt = Some(Instant.now()),
      updatedAt = Instant.now()
    )

  /** 更新信息 */
  def updateInfo(name: Option[String], description: Option[String], avatar: Option[String]): Organization =
    copy(
      name = name.getOrElse(this.name),
      description = description.orElse(this.description),
      avatar = avatar.orElse(this.avatar),
      updatedAt = Instant.now()
    )

object Organization:
  /** 创建免费组织 */
  def createFree(
    id: OrganizationId,
    name: String,
    description: Option[String] = None,
    avatar: Option[String] = None
  ): Organization =
    val now = Instant.now()
    Organization(
      id = id,
      name = name,
      description = description,
      avatar = avatar,
      planType = PlanType.Free,
      quota = OrganizationQuota.free,
      isActive = true,
      isDeleted = false,
      createdAt = now,
      updatedAt = now,
      deletedAt = None
    )
