package app.mosia.nexus
package presentation.http.endpoint

import domain.services.app.UserService
import domain.services.infra.JwtService
import application.dto.request.user.UserCreateRequest
import application.dto.response.common.ApiResponse
import application.dto.response.user.UserResponse
import domain.error.*

import presentation.http.endpoint.SecureEndpoints.{baseEndpoint, secureBase}
import sttp.capabilities.zio.ZioStreams
import sttp.model.StatusCode
import sttp.tapir.{Endpoint, EndpointOutput}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZIO

final class UserEndpoint(userService: UserService) extends EndpointModule:
  override def publicEndpoints: List[ZServerEndpoint[Any, ZioStreams]] =
    List(createUser)

  override def secureEndpoints: List[ZServerEndpoint[JwtService, ZioStreams]] =
    List(getProfile)

  private val createUser: ZServerEndpoint[Any, ZioStreams] =
    baseEndpoint.post
      .in("api" / "users")
      .in(jsonBody[UserCreateRequest])
      .out(jsonBody[ApiResponse[UserResponse]])
      .zServerLogic { request =>
        (for
          user <- userService.createUser(request.email, request.password)
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

  private val getProfile: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.get
      .in("api" / "me")
      .out(jsonBody[ApiResponse[UserResponse]])
      .serverLogic { userId => _ =>
        (for
          user <- userService.findById(userId).someOrFail(NotFound("User", userId.value.toString))
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
