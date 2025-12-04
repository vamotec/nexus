package app.mosia.nexus
package presentation.http.v1

import application.dto.response.common.ApiResponse
import application.dto.response.user.UserResponse
import domain.error.*
import domain.services.app.UserService
import domain.services.infra.JwtContent
import app.mosia.nexus.presentation.http.EndpointModule
import app.mosia.nexus.presentation.http.SecureEndpoints.secureBase

import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class UserEndpoint(userService: UserService) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    List.empty

  override def secureEndpoints: List[ZServerEndpoint[JwtContent, ZioStreams]] =
    List(getProfile)

  private val getProfile: ZServerEndpoint[JwtContent, ZioStreams] =
    secureBase.get
      .in("me")
      .out(jsonBody[ApiResponse[UserResponse]])
      .serverLogic { userId => _ =>
        (for
          user <- userService.findById(userId).someOrFail(NotFound("User", userId))
          response = UserResponse(
            id = user.id.value.toString,
            username = user.name,
            email = user.email,
            avatar = user.avatar,
            role = user.role.toString,
            createdAt = user.createdAt.toString
          )
        yield ApiResponse(data = response)).mapError(toErrorResponse)
      }
