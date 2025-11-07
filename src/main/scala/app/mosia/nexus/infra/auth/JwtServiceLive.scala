package app.mosia.nexus.infra.auth

import app.mosia.nexus.domain.model.user.{User, UserId}
import app.mosia.nexus.domain.repository.UserRepository
import app.mosia.nexus.infra.config.TokenConfig
import app.mosia.nexus.infra.error.*
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtZIOJson}
import zio.*
import zio.json.{DecoderOps, EncoderOps}

import java.time.Clock
import java.util.UUID
import scala.util.Try

final class JwtServiceLive(config: TokenConfig) extends JwtService:
  // jwt-scala 库需要一个时钟来处理过期时间
  private implicit val clock: Clock = Clock.systemUTC()
  // 我们选择 HMAC SHA256 作为加密算法
  private val algorithm = JwtAlgorithm.HS256

  override def generateToken(userId: UserId, platform: Option[String] = None): AppTask[String] = ZIO.succeed {
    val expiration = platform match
      case Some("ios") | Some("android") =>
        Some(clock.instant().plusSeconds(30.days.toSeconds).getEpochSecond)
      case _ =>
        Some(clock.instant().plusSeconds(config.expiration.accessToken.toSeconds).getEpochSecond)
    val claim = JwtClaim(
      content = JwtPayload(userId.value.toString).toJson,
      expiration = expiration,
      issuedAt = Some(clock.instant().getEpochSecond)
    )
    JwtZIOJson.encode(claim, config.secret, algorithm)
  }

  override def decode(token: String): AppTask[JwtClaim] =
    ZIO
      .fromTry(JwtZIOJson.decode(token, config.secret, Seq(algorithm)))
      .mapError(ex => InvalidToken(s"Invalid token: ${ex.getMessage}"))

  override def validateToken(token: String): AppTask[UserId] =
    for {
      claim <- decode(token)
      now = clock.instant().getEpochSecond

      _ <- ZIO.when(claim.expiration.exists(_ < now))(
        ZIO.fail(TokenExpired)
      )

      payload <- ZIO
        .fromEither(claim.content.fromJson[JwtPayload])
        .mapError(err => InvalidToken(s"Failed to parse token payload: $err"))

      userId <- UserId
        .fromStringZIO(payload.userIdStr)
        .mapError(ex => InvalidToken(s"Invalid user id in token payload: ${ex.getMessage}"))
    } yield userId

object JwtServiceLive:
  val live: ZLayer[TokenConfig, Nothing, JwtService] =
    ZLayer.fromFunction(new JwtServiceLive(_))
