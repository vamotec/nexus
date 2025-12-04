package app.mosia.nexus
package domain.services.app

import domain.error.AppTask
import domain.model.organization.{Organization, OrganizationId, OrganizationMember, OrganizationRole}
import domain.model.user.UserId
import zio.*

trait OrganizationService:
  /** 创建组织 */
  def createOrganization(name: String, description: Option[String], createdBy: String): AppTask[Organization]

  /** 获取组织详情 */
  def getOrganization(organizationId: String, userId: String): AppTask[Organization]

  /** 列出用户拥有的组织 */
  def listOwnedOrganizations(userId: String): AppTask[List[Organization]]

  /** 列出用户加入的所有组织 */
  def listMemberOrganizations(userId: String): AppTask[List[Organization]]

  /** 更新组织信息 */
  def updateOrganization(
    organizationId: String,
    userId: String,
    name: Option[String],
    description: Option[String],
    avatar: Option[String]
  ): AppTask[Organization]

  /** 删除组织 */
  def deleteOrganization(organizationId: String, userId: String): AppTask[Unit]

  /** 邀请成员加入组织 */
  def inviteMember(
    organizationId: String,
    invitedUserId: String,
    invitedBy: String,
    role: OrganizationRole
  ): AppTask[OrganizationMember]

  /** 移除组织成员 */
  def removeMember(organizationId: String, userId: String, removedBy: String): AppTask[Unit]

  /** 接受组织邀请 */
  def acceptInvite(organizationId: String, userId: String): AppTask[Unit]

  /** 离开组织 */
  def leaveOrganization(organizationId: String, userId: String): AppTask[Unit]

  /** 转让组织所有权 */
  def transferOwnership(
    organizationId: String,
    currentOwnerId: String,
    newOwnerId: String
  ): AppTask[Unit]

  /** 更新成员角色 */
  def updateMemberRole(
    organizationId: String,
    targetUserId: String,
    newRole: OrganizationRole,
    updatedBy: String
  ): AppTask[Unit]

  /** 列出组织成员 */
  def listMembers(organizationId: String, userId: String): AppTask[List[OrganizationMember]]

  /** 列出待处理的邀请 */
  def listPendingInvites(userId: String): AppTask[List[OrganizationMember]]
