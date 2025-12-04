package app.mosia.nexus
package domain.model.organization

import domain.model.user.UserId

import java.time.Instant
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*

/** 组织成员
  *
  * @param organizationId 组织 ID
  * @param userId 用户 ID
  * @param role 角色 (Owner, Admin, Member)
  * @param isActive 是否仍为成员
  * @param isInvited 是否为邀请状态
  * @param joinedAt 加入时间
  * @param leftAt 离开时间
  * @param invitedBy 邀请者 ID
  * @param invitedAt 邀请时间
  */
case class OrganizationMember(
  organizationId: OrganizationId,
  userId: UserId,
  role: OrganizationRole,
  isActive: Boolean,
  isInvited: Boolean,
  joinedAt: Instant,
  leftAt: Option[Instant],
  invitedBy: Option[UserId],
  invitedAt: Option[Instant]
) derives JsonCodec:

  /** 是否为所有者 */
  def isOwner: Boolean = role == OrganizationRole.Owner

  /** 是否为管理员 */
  def isAdmin: Boolean = role == OrganizationRole.Admin

  /** 是否有管理权限 (Owner 或 Admin) */
  def hasAdminPermission: Boolean = isOwner || isAdmin

  /** 检查是否可以邀请新成员 */
  def canInviteMembers: Boolean = isActive && hasAdminPermission

  /** 检查是否可以移除成员 */
  def canRemoveMember(targetRole: OrganizationRole): Boolean =
    isActive && (
      (isOwner) || // Owner 可以移除任何人
      (isAdmin && targetRole == OrganizationRole.Member) // Admin 只能移除普通成员
    )

  /** 检查是否可以转让所有权 */
  def canTransferOwnership: Boolean = isActive && isOwner

  /** 接受邀请 */
  def acceptInvite: OrganizationMember =
    copy(isInvited = false, joinedAt = Instant.now())

  /** 离开组织 */
  def leave: OrganizationMember =
    copy(isActive = false, leftAt = Some(Instant.now()))

  /** 更新角色 */
  def updateRole(newRole: OrganizationRole): OrganizationMember =
    copy(role = newRole)
