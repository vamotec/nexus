package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.organization.{Organization, OrganizationId, PlanType}
import domain.model.user.UserId

import java.util.UUID

trait OrganizationRepository:
  /** 创建组织 */
  def create(organization: Organization): AppTask[Unit]

  /** 根据 ID 查找组织 */
  def findById(id: OrganizationId): AppTask[Option[Organization]]

  /** 根据名称查找组织 */
  def findByName(name: String): AppTask[Option[Organization]]

  /** 更新组织信息 */
  def update(organization: Organization): AppTask[Unit]

  /** 软删除组织 */
  def softDelete(id: OrganizationId): AppTask[Long]

  /** 列出用户拥有的组织 */
  def listByOwner(userId: UserId): AppTask[List[Organization]]

  /** 列出用户加入的所有组织（包括拥有的） */
  def listByMember(userId: UserId): AppTask[List[Organization]]

  /** 统计用户拥有的组织数量 */
  def countByOwner(userId: UserId): AppTask[Int]

  /** 统计组织的成员数量 */
  def countMembers(organizationId: OrganizationId): AppTask[Int]

  /** 检查组织名称是否已存在 */
  def existsByName(name: String): AppTask[Boolean]

  /** 检查用户是否为组织成员 */
  def isMember(organizationId: OrganizationId, userId: UserId): AppTask[Boolean]
