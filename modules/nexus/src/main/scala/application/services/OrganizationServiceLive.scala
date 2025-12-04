package app.mosia.nexus
package application.services

import domain.error.*
import domain.model.organization.*
import domain.model.user.UserId
import domain.repository.{OrganizationRepository, OrganizationMemberRepository, UserRepository}
import domain.services.app.OrganizationService

import zio.*

import java.time.Instant
import java.util.UUID

final class OrganizationServiceLive(
  orgRepo: OrganizationRepository,
  memberRepo: OrganizationMemberRepository,
  userRepo: UserRepository
) extends OrganizationService:

  /** 创建组织 */
  override def createOrganization(
    name: String,
    description: Option[String],
    createdBy: String
  ): AppTask[Organization] =
    for
      createById <- UserId.fromString(createdBy)
      // 1. 检查用户配额
      user <- userRepo
        .findById(createById)
        .someOrFail(NotFound("User", createById.value.toString))

      _ <- ZIO
        .fail(BusinessRuleViolation(
          "Maximum organizations",
          s"The number of organizations: ${user.quota.currentOwnedOrganizations} " +
            s"exceeds the maximum limit for free account: ${user.quota.maxOwnedOrganizations} ."))
        .when(!user.quota.canCreateOrganization)

      // 2. 检查组织名称是否已存在
      nameExists <- orgRepo.existsByName(name)
      _ <- ZIO
        .fail(AlreadyExists("Organization name", name))
        .when(nameExists)

      // 3. 创建组织
      organization = Organization.createFree(
        id = OrganizationId(UUID.randomUUID()),
        name = name,
        description = description,
        avatar = None
      )

      // 4. 创建组织所有者成员关系
      now = Instant.now()
      ownerMember = OrganizationMember(
        organizationId = organization.id,
        userId = createById,
        role = OrganizationRole.Owner,
        isActive = true,
        isInvited = false,
        joinedAt = now,
        leftAt = None,
        invitedBy = None,
        invitedAt = None
      )

      // 5. 在事务中保存组织和成员关系
      _ <- orgRepo.create(organization)
      _ <- memberRepo.add(ownerMember)

      // 6. 更新用户配额（增加拥有的组织数）
      // 注意：这里需要在 UserRepository 中添加更新配额的方法
      _ <- ZIO.logInfo(s"User ${createById.value} created organization ${organization.id.value}")
    yield organization

  /** 获取组织详情 */
  override def getOrganization(organizationId: String, userId: String): AppTask[Organization] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 查询组织
      organization <- orgRepo
        .findById(orgId)
        .someOrFail(NotFound("Organization", orgId.value.toString))

      // 2. 验证用户是否为组织成员
      isMember <- orgRepo.isMember(orgId, uId)
      _ <- ZIO
        .fail(PermissionDenied("view", "organization"))
        .when(!isMember)
    yield organization

  /** 列出用户拥有的组织 */
  override def listOwnedOrganizations(userId: String): AppTask[List[Organization]] =
    for
      uId <- UserId.fromString(userId)
      org <- orgRepo.listByOwner(uId)
    yield org

  /** 列出用户加入的所有组织 */
  override def listMemberOrganizations(userId: String): AppTask[List[Organization]] =
    for
      uId <- UserId.fromString(userId)
      org <-  orgRepo.listByMember(uId)
    yield org
   

  /** 更新组织信息 */
  override def updateOrganization(
    organizationId: String,
    userId: String,
    name: Option[String],
    description: Option[String],
    avatar: Option[String]
  ): AppTask[Organization] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 查询组织
      organization <- orgRepo
        .findById(orgId)
        .someOrFail(NotFound("Organization", orgId.value.toString))

      // 2. 检查权限（只有 Owner 和 Admin 可以更新）
      member <- memberRepo
        .find(orgId, uId)
        .someOrFail(PermissionDenied("update", "organization"))

      _ <- ZIO
        .fail(PermissionDenied("update", "organization"))
        .when(!member.hasAdminPermission)

      // 3. 如果修改名称，检查新名称是否已存在
      _ <- name match
        case Some(newName) if newName != organization.name =>
          orgRepo.existsByName(newName).flatMap { exists =>
            ZIO.fail(AlreadyExists("Organization name", newName)).when(exists)
          }
        case _ => ZIO.unit

      // 4. 更新组织信息
      updated = organization.updateInfo(name, description, avatar)
      _ <- orgRepo.update(updated)
    yield updated

  /** 删除组织 */
  override def deleteOrganization(organizationId: String, userId: String): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 检查权限（只有 Owner 可以删除）
      member <- memberRepo
        .find(orgId, uId)
        .someOrFail(PermissionDenied("delete", "organization"))

      _ <- ZIO
        .fail(PermissionDenied("delete", "organization"))
        .when(!member.isOwner)

      // 2. 软删除组织
      _ <- orgRepo.softDelete(orgId)

      _ <- ZIO.logInfo(s"Organization ${orgId.value} deleted by user ${uId.value}")
    yield ()

  /** 邀请成员加入组织 */
  override def inviteMember(
    organizationId: String,
    invitedUserId: String,
    invitedBy: String,
    role: OrganizationRole
  ): AppTask[OrganizationMember] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      invitedUId <- UserId.fromString(invitedUserId)
      invitedById <- UserId.fromString(invitedBy)
      // 1. 检查邀请者权限（Admin 和 Owner 可以邀请）
      inviter <- memberRepo
        .find(orgId, invitedById)
        .someOrFail(PermissionDenied("invite", "members"))

      _ <- ZIO
        .fail(PermissionDenied("invite", "members"))
        .when(!inviter.hasAdminPermission)

      // 2. 检查被邀请用户是否已经是成员
      existingMember <- memberRepo.find(orgId, invitedUId)
      _ <- ZIO
        .fail(AlreadyExists("OrganizationMember(user)", invitedUId.value.toString))
        .when(existingMember.exists(_.isActive))

      // 3. 检查组织配额
      organization <- orgRepo
        .findById(orgId)
        .someOrFail(NotFound("Organization", orgId.value.toString))

      memberCount <- memberRepo.countActiveMembers(orgId)
      _ <- ZIO
        .fail(BusinessRuleViolation(
          "Maximum members", 
          s"The number of organization members: $memberCount exceeds the maximum limit: ${organization.quota.maxUsers} ."))
        .when(!organization.canAddMember(memberCount))

      // 4. 创建邀请
      now = Instant.now()
      invite = OrganizationMember(
        organizationId = orgId,
        userId = invitedUId,
        role = role,
        isActive = true,
        isInvited = true,
        joinedAt = now,
        leftAt = None,
        invitedBy = Some(invitedById),
        invitedAt = Some(now)
      )

      _ <- memberRepo.add(invite)
      _ <- ZIO.logInfo(s"User ${invitedUId.value} invited to organization ${orgId.value}")
    yield invite

  /** 移除组织成员 */
  override def removeMember(organizationId: String, userId: String, removedBy: String): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      removedById <- UserId.fromString(removedBy)
      uId <- UserId.fromString(userId)
      // 1. 检查移除者权限
      remover <- memberRepo
        .find(orgId, removedById)
        .someOrFail(PermissionDenied("remove", "members"))

      // 2. 检查被移除的成员
      targetMember <- memberRepo
        .find(orgId, uId)
        .someOrFail(NotFound("OrganizationMember", s"org=${orgId.value}, user=${uId.value}"))

      // 3. 不能移除自己（应该使用 leaveOrganization）
      _ <- ZIO
        .fail(BusinessRuleViolation("remove member", "Cannot remove yourself, use leaveOrganization instead"))
        .when(uId == removedById)

      // 4. 不能移除 Owner
      _ <- ZIO
        .fail(BusinessRuleViolation("remove member", "Cannot remove organization owner, transfer ownership first"))
        .when(targetMember.isOwner)

      // 5. 检查权限
      _ <- ZIO
        .fail(PermissionDenied("remove", "members"))
        .when(!remover.canRemoveMember(targetMember.role))

      // 6. 移除成员
      _ <- memberRepo.remove(orgId, uId)
      _ <- ZIO.logInfo(s"User ${uId.value} removed from organization ${orgId.value}")
    yield ()

  /** 接受组织邀请 */
  override def acceptInvite(organizationId: String, userId: String): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 查找邀请
      member <- memberRepo
        .find(orgId, uId)
        .someOrFail(NotFound("Invitation", s"org=${orgId.value}, user=${uId.value}"))

      _ <- ZIO
        .fail(BusinessRuleViolation("Invitation", "No pending invitation"))
        .when(!member.isInvited)

      // 2. 接受邀请
      _ <- memberRepo.acceptInvite(orgId, uId)
      _ <- ZIO.logInfo(s"User ${uId.value} accepted invitation to organization ${orgId.value}")
    yield ()

  /** 离开组织 */
  override def leaveOrganization(organizationId: String, userId: String): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 检查成员关系
      member <- memberRepo
        .find(orgId, uId)
        .someOrFail(NotFound("OrganizationMember", s"org=${orgId.value}, user=${uId.value}"))

      // 2. Owner 不能直接离开，需要先转让所有权
      _ <- ZIO
        .fail(BusinessRuleViolation("leave organization", "Owner cannot leave organization, transfer ownership first"))
        .when(member.isOwner)

      // 3. 移除成员
      _ <- memberRepo.remove(orgId, uId)
      _ <- ZIO.logInfo(s"User ${uId.value} left organization ${orgId.value}")
    yield ()

  /** 转让组织所有权 */
  override def transferOwnership(
    organizationId: String,
    currentOwnerId: String,
    newOwnerId: String
  ): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      currentId <- UserId.fromString(currentOwnerId)
      newId <- UserId.fromString(newOwnerId)
      // 1. 检查当前所有者权限
      currentOwner <- memberRepo
        .find(orgId, currentId)
        .someOrFail(PermissionDenied("transfer", "ownership"))

      _ <- ZIO
        .fail(PermissionDenied("transfer", "ownership"))
        .when(!currentOwner.isOwner)

      // 2. 检查新所有者是否为组织成员
      newOwner <- memberRepo
        .find(orgId, newId)
        .someOrFail(NotFound("OrganizationMember", s"org=${orgId.value}, user=${newId.value}"))

      _ <- ZIO
        .fail(BusinessRuleViolation("Transfer owner", "New owner must be an active member"))
        .when(!newOwner.isActive)

      // 3. 检查新所有者的配额
      user <- userRepo
        .findById(newId)
        .someOrFail(NotFound("User", newId.value.toString))

      _ <- ZIO
        .fail(BusinessRuleViolation(
          "Maximum organizations",
          s"The number of organizations: ${user.quota.currentOwnedOrganizations} " +
            s"exceeds the maximum limit for free account: ${user.quota.maxOwnedOrganizations} ."))
        .when(!user.quota.canCreateOrganization)

      // 4. 更新角色
      _ <- memberRepo.updateRole(orgId, currentId, OrganizationRole.Admin)
      _ <- memberRepo.updateRole(orgId, newId, OrganizationRole.Owner)

      _ <- ZIO.logInfo(s"Organization ${orgId.value} ownership transferred from ${currentId.value} to ${newId.value}")
    yield ()

  /** 更新成员角色 */
  override def updateMemberRole(
    organizationId: String,
    targetUserId: String,
    newRole: OrganizationRole,
    updatedBy: String
  ): AppTask[Unit] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      targetId <- UserId.fromString(targetUserId)
      updatedById <- UserId.fromString(updatedBy)
      // 1. 检查更新者权限
      updater <- memberRepo
        .find(orgId, updatedById)
        .someOrFail(PermissionDenied("update", "member role"))

      _ <- ZIO
        .fail(PermissionDenied("update", "member role"))
        .when(!updater.isOwner) // 只有 Owner 可以修改角色

      // 2. 检查目标成员
      targetMember <- memberRepo
        .find(orgId, targetId)
        .someOrFail(NotFound("OrganizationMember", s"org=${orgId.value}, user=${targetId.value}"))

      // 3. 不能修改 Owner 的角色（应该使用 transferOwnership）
      _ <- ZIO
        .fail(BusinessRuleViolation("update member role", "Cannot change owner role, use transferOwnership instead"))
        .when(targetMember.isOwner)

      // 4. 不能将成员提升为 Owner（应该使用 transferOwnership）
      _ <- ZIO
        .fail(BusinessRuleViolation("update member role", "Use transferOwnership to assign owner role"))
        .when(newRole == OrganizationRole.Owner)

      // 5. 更新角色
      _ <- memberRepo.updateRole(orgId, targetId, newRole)
      _ <- ZIO.logInfo(s"Member ${targetId.value} role updated to $newRole in organization ${orgId.value}")
    yield ()

  /** 列出组织成员 */
  override def listMembers(organizationId: String, userId: String): AppTask[List[OrganizationMember]] =
    for
      orgId <- OrganizationId.fromString(organizationId)
      uId <- UserId.fromString(userId)
      // 1. 验证用户是否为组织成员
      isMember <- orgRepo.isMember(orgId, uId)
      _ <- ZIO
        .fail(PermissionDenied("view", "members"))
        .when(!isMember)

      // 2. 列出所有成员
      members <- memberRepo.listByOrganization(orgId)
    yield members.filter(_.isActive) // 只返回激活的成员

  /** 列出待处理的邀请 */
  override def listPendingInvites(userId: String): AppTask[List[OrganizationMember]] =
    for
      uId <- UserId.fromString(userId)
      member <- memberRepo.listPendingInvites(uId)
    yield member

object OrganizationServiceLive:
  val live: ZLayer[OrganizationRepository & OrganizationMemberRepository & UserRepository, Nothing, OrganizationService] =
    ZLayer.fromFunction(OrganizationServiceLive(_, _, _))
