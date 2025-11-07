package app.mosia.nexus.presentation.http.endpoint

import app.mosia.nexus.application.dto.request.user.UserCreateRequest
import app.mosia.nexus.application.dto.response.common.ApiResponse
import app.mosia.nexus.application.dto.response.user.UserResponse
import app.mosia.nexus.application.service.user.UserService
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.error.*
import app.mosia.nexus.presentation.http.endpoint.SecureEndpoints.{baseEndpoint, secureBase}
import sttp.capabilities.zio.ZioStreams
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.{RichZEndpoint, ZServerEndpoint, stringToPath}

final class UserEndpoint(userService: UserService) extends EndpointModule:
  override def endpoints: List[ZServerEndpoint[JwtService, ZioStreams]] =
    List(createUser)

  val createUser: ZServerEndpoint[JwtService, ZioStreams] =
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
            avatar = Some(user.avatar),
            role = user.role.toString,
            createdAt = user.createdAt.toString
          )
        yield ApiResponse(data = response)).mapError(ErrorMapper.toErrorResponse)
      }

  val getProfile: ZServerEndpoint[JwtService, ZioStreams] =
    secureBase.get
      .in("api" / "me")
      .out(jsonBody[ApiResponse[UserResponse]])
      .serverLogic { userId => _ =>
        (for
          user <- userService.findById(userId).someOrFail(UserNotFound)
          response = UserResponse(
            id = user.id.value.toString,
            username = user.name,
            email = user.email,
            avatar = Some(user.avatar),
            role = user.role.toString,
            createdAt = user.createdAt.toString
          )
        yield ApiResponse(data = response)).mapError(ErrorMapper.toErrorResponse)
      }
