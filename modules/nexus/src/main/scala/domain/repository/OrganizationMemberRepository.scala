package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.organization.{OrganizationId, OrganizationMember, OrganizationRole}
import domain.model.user.UserId

import java.util.UUID

trait OrganizationMemberRepository:
  /** 添加成员 */
  def add(member: OrganizationMember): AppTask[Unit]

  /** 更新成员信息 */
  def update(member: OrganizationMember): AppTask[Unit]

  /** 移除成员 */
  def remove(organizationId: OrganizationId, userId: UserId): AppTask[Long]

  /** 查找成员 */
  def find(organizationId: OrganizationId, userId: UserId): AppTask[Option[OrganizationMember]]

  /** 列出组织的所有成员 */
  def listByOrganization(organizationId: OrganizationId): AppTask[List[OrganizationMember]]

  /** 列出用户加入的所有组织成员关系 */
  def listByUser(userId: UserId): AppTask[List[OrganizationMember]]

  /** 查找组织的所有者 */
  def findOwners(organizationId: OrganizationId): AppTask[List[OrganizationMember]]

  /** 统计组织的激活成员数 */
  def countActiveMembers(organizationId: OrganizationId): AppTask[Int]

  /** 统计用户拥有的组织数（Owner 角色） */
  def countOwnedOrganizations(userId: UserId): AppTask[Int]

  /** 更新成员角色 */
  def updateRole(organizationId: OrganizationId, userId: UserId, role: OrganizationRole): AppTask[Unit]

  /** 接受邀请 */
  def acceptInvite(organizationId: OrganizationId, userId: UserId): AppTask[Unit]

  /** 列出待处理的邀请 */
  def listPendingInvites(userId: UserId): AppTask[List[OrganizationMember]]
