package app.mosia.nexus
package presentation.http.v1

import application.dto.request.organization.{
  CreateOrganizationRequest,
  InviteMemberRequest,
  TransferOwnershipRequest,
  UpdateOrganizationRequest
}
import application.dto.response.common.ApiResponse
import application.dto.response.organization.{OrganizationMemberResponse, OrganizationResponse, OrganizationsResponse}
import domain.error.*
import domain.model.organization.OrganizationRole
import domain.services.app.OrganizationService
import domain.services.infra.JwtContent
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.secureBase

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class OrganizationEndpoint(service: OrganizationService) extends EndpointModule:

  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] = List.empty

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] =
    List(
      createOrganization,
      listOrganizations,
      listOwnedOrganizations,
      getOrganization,
      updateOrganization,
      deleteOrganization,
      listMembers,
      inviteMember,
      removeMember,
      acceptInvite,
      leaveOrganization,
      transferOwnership,
      listPendingInvites
    )


  // ========== Endpoints ==========
  private val organizationEndpoint=
    secureBase
      .in("organizations")

  
  /** POST /api/organizations - 创建组织 */
  private val createOrganization: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.post
      .in(jsonBody[CreateOrganizationRequest])
      .out(jsonBody[ApiResponse[OrganizationResponse]])
      .serverLogic { userId => req =>
        (for 
          org <- service
            .createOrganization(req.name, req.description, userId)
            .map(OrganizationResponse.fromDomain)
        yield ApiResponse(data = org)).mapError(toErrorResponse)
      }

  /** GET /api/organizations - 列出用户的所有组织 */
  private val listOrganizations: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.get
      .out(jsonBody[ApiResponse[List[OrganizationsResponse]]])
      .serverLogic { userId => _ =>
        (for
          orgs <- service
            .listMemberOrganizations(userId)
            .map(_.map(OrganizationsResponse.fromDomain))
        yield ApiResponse(data = orgs)).mapError(toErrorResponse)
      }

  /** GET /api/organizations/owned - 列出用户拥有的组织 */
  private val listOwnedOrganizations: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.get
      .in("owned")
      .out(jsonBody[ApiResponse[List[OrganizationsResponse]]])
      .serverLogic { userId => _ =>
        (for
          orgs <- service
            .listOwnedOrganizations(userId)
            .map(_.map(OrganizationsResponse.fromDomain))
        yield ApiResponse(data = orgs)).mapError(toErrorResponse)
      }

  /** GET /api/v1/organizations/:id - 获取组织详情 */
  private val getOrganization: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.get
      .in(path[String]("organizationId"))
      .out(jsonBody[ApiResponse[OrganizationResponse]])
      .serverLogic { userId => orgId =>
        (for
          orgs <- service
            .getOrganization(orgId, userId)
            .map(OrganizationResponse.fromDomain)
        yield ApiResponse(data = orgs)).mapError(toErrorResponse)
      }

  /** PUT /api/v1/organizations/:id - 更新组织信息 */
  private val updateOrganization: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.put
      .in(path[String]("organizationId"))
      .in(jsonBody[UpdateOrganizationRequest])
      .out(jsonBody[ApiResponse[OrganizationResponse]])
      .serverLogic { userId => (orgId, req) =>
        (for
          orgs <- service
            .updateOrganization(orgId, userId, req.name, req.description, req.avatar)
            .map(OrganizationResponse.fromDomain)
        yield ApiResponse(data = orgs)).mapError(toErrorResponse)
      }

  /** DELETE /api/v1/organizations/:id - 删除组织 */
  private val deleteOrganization: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.delete
      .in(path[String]("organizationId"))
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .serverLogic { userId => orgId =>
        (for
          _ <- service.deleteOrganization(orgId, userId)
        yield ())
          .mapError(toErrorResponse)
      }

  /** GET /api/v1/organizations/:id/members - 列出组织成员 */
  private val listMembers: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.get
      .in(path[String]("organizationId") / "members")
      .out(jsonBody[ApiResponse[List[OrganizationMemberResponse]]])
      .serverLogic { userId => orgId =>
        (for
          members <- service
            .listMembers(orgId, userId)
            .map(_.map(OrganizationMemberResponse.fromDomain))
        yield ApiResponse(data = members)).mapError(toErrorResponse)
      }

  /** POST /api/v1/organizations/:id/members - 邀请成员 */
  private val inviteMember: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.post
      .in(path[String]("organizationId") / "members")
      .in(jsonBody[InviteMemberRequest])
      .out(jsonBody[ApiResponse[OrganizationMemberResponse]])
      .serverLogic { userId => (orgId, req) =>
        (for
          role <- ZIO.attempt(OrganizationRole.fromString(req.role)).orElseFail(InvalidInput("role", "Invalid role", Some(req.role)))
          member <- service.inviteMember(orgId, req.userId, userId, role)
            .map(OrganizationMemberResponse.fromDomain)
        yield ApiResponse(data = member)).mapError(toErrorResponse)
      }

  /** DELETE /api/v1/organizations/:id/members/:userId - 移除成员 */
  private val removeMember: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.delete
      .in(path[String]("organizationId") / "members" / path[String]("userId"))
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .serverLogic { userId => (orgId, targetUserId) =>
        (for
          _ <- service.removeMember(orgId, targetUserId, userId)
        yield ())
          .mapError(toErrorResponse)
      }

  /** POST /api/v1/organizations/:id/accept - 接受邀请 */
  private val acceptInvite: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.post
      .in(path[String]("organizationId") / "accept")
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .serverLogic { userId => orgId =>
        (for
          _ <- service.acceptInvite(orgId, userId)
        yield ())
          .mapError(toErrorResponse)
      }

  /** POST /api/v1/organizations/:id/leave - 离开组织 */
  private val leaveOrganization: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.post
      .in(path[String]("organizationId") / "leave")
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .serverLogic { userId => orgId =>
        (for
          _ <- service.leaveOrganization(orgId, userId)
        yield ())
          .mapError(toErrorResponse)
      }

  /** POST /api/v1/organizations/:id/transfer-ownership - 转让所有权 */
  private val transferOwnership: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.post
      .in(path[String]("organizationId") / "transfer-ownership")
      .in(jsonBody[TransferOwnershipRequest])
      .out(statusCode(sttp.model.StatusCode.NoContent))
      .serverLogic { userId => (orgId, req) =>
        (for
          _ <- service.transferOwnership(orgId, userId, req.newOwnerId)
        yield ())
          .mapError(toErrorResponse)
      }

  /** GET /api/v1/organizations/invites - 列出待处理的邀请 */
  private val listPendingInvites: ZServerEndpoint[JwtContent, ZioStreams] =
    organizationEndpoint.get
      .in("invites")
      .out(jsonBody[ApiResponse[List[OrganizationMemberResponse]]])
      .serverLogic { userId => _ =>
        (for
          members <- service
            .listPendingInvites(userId)
            .map(_.map(OrganizationMemberResponse.fromDomain))
        yield ApiResponse(data = members)).mapError(toErrorResponse)
      }