package app.mosia.nexus
package infrastructure.jwt

import domain.config.AppConfig
import domain.error.*
import domain.model.jwt.JwtPayload
import domain.model.project.ProjectId
import domain.model.session.SessionId
import domain.model.user.UserId
import domain.services.infra.JwtService

import pdi.jwt.*

import zio.*
import zio.json.*
import zio.http.*
import zio.json.ast.Json

import java.time.Clock
import java.util.UUID

final class JwtServiceLive(config: AppConfig) extends JwtService:
  // jwt-scala 库需要一个时钟来处理过期时间
  private implicit val clock: Clock = Clock.systemUTC()
  // 我们选择 HMAC SHA256 作为加密算法
  private val algorithm = JwtAlgorithm.HS256

  override def generateToken(userId: UserId, platform: Option[String] = None): AppTask[String] = ZIO.succeed {
    val expiration = platform match
      case Some("ios") | Some("android") =>
        Some(clock.instant().plusSeconds(30.days.toSeconds).getEpochSecond)
      case _ =>
        Some(clock.instant().plusSeconds(config.auth.token.expiration.accessToken.toSeconds).getEpochSecond)
    val claim = JwtClaim(
      content = JwtPayload(userId.value.toString).toJson,
      issuer = Some(config.auth.token.issuer),
      audience = Some(Set(config.auth.token.audience)),
      expiration = expiration,
      issuedAt = Some(clock.instant().getEpochSecond)
    )
    JwtZIOJson.encode(claim, config.auth.token.secret, algorithm)
  }

  override def decode(token: String): AppTask[JwtClaim] =
    ZIO
      .fromTry(JwtZIOJson.decode(token, config.auth.token.secret, Seq(algorithm)))
      .mapError(ex => InvalidInput("jwt", s"Invalid token: ${ex.getMessage}"))

  override def validateToken(token: String): AppTask[UserId] =
    for {
      claim <- decode(token)
      now = clock.instant().getEpochSecond

      _ <- ZIO.when(claim.expiration.exists(_ < now))(
        ZIO.fail(
          TokenExpired(
            context = Map("expiration" -> claim.expiration.toString)
          )
        )
      )

      payload <- ZIO
        .fromEither(claim.content.fromJson[JwtPayload])
        .mapError(err => InvalidInput("jwtPayload", s"Failed to parse token payload: $err"))

      userId <- UserId
        .fromString(payload.userIdStr)
        .mapError(ex => InvalidInput("UserId", s"Invalid user id in token payload: ${ex.getMessage}"))
    } yield userId

  override def generateSessionToken(sessionId: SessionId, userId: UserId, permissions: Set[String]): AppTask[String] =
    ZIO
      .attempt:
        val now       = clock.instant()
        val expiresAt = now.plusSeconds(config.auth.token.expiration.controlToken.toSeconds)

        val claim = JwtClaim(
          content = Json
            .Obj(
              "user_id" -> Json.Str(userId.value.toString),
              "session_id" -> Json.Str(sessionId.value.toString),
              "permissions" -> Json.Arr(permissions.map(Json.Str(_)).toSeq*),
              "type" -> Json.Str("control_token")
            )
            .toJson,
          issuer = Some(config.auth.token.issuer),
          audience = Some(Set(config.auth.token.audience)),
          issuedAt = Some(now.getEpochSecond),
          expiration = Some(expiresAt.getEpochSecond)
        )

        JwtZIOJson.encode(claim, config.auth.token.secret, JwtAlgorithm.HS256)
      .mapError(err => UnexpectedError(Some(err)))

  override def generateOmniverseToken(projectId: ProjectId, userId: UserId, permissions: Set[String]): AppTask[String] =
    ZIO
      .attempt:
        val now       = clock.instant()
        val expiresAt = now.plusSeconds(config.auth.token.expiration.controlToken.toSeconds)

        val claim = JwtClaim(
          content = Json
            .Obj(
              "user_id" -> Json.Str(userId.value.toString),
              "session_id" -> Json.Str(projectId.value.toString),
              "permissions" -> Json.Arr(permissions.map(Json.Str(_)).toSeq*),
              "type" -> Json.Str("control_token")
            )
            .toJson,
          issuer = Some(config.auth.token.issuer),
          audience = Some(Set(config.auth.token.audience)),
          issuedAt = Some(now.getEpochSecond),
          expiration = Some(expiresAt.getEpochSecond)
        )

        JwtZIOJson.encode(claim, config.auth.token.secret, JwtAlgorithm.HS256)
      .mapError(err => UnexpectedError(Some(err)))

object JwtServiceLive:
  val live: ZLayer[AppConfig, Nothing, JwtService] =
    ZLayer.fromFunction(new JwtServiceLive(_))
