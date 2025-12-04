package app.mosia.nexus
package infrastructure.jwt

import domain.config.AppConfig
import domain.error.*
import domain.model.jwt.Payload
import domain.services.infra.JwtService

import pdi.jwt.*
import zio.*
import zio.json.*

import java.time.Clock

final class JwtServiceLive(config: AppConfig) extends JwtService:
  // jwt-scala 库需要一个时钟来处理过期时间
  private implicit val clock: Clock = Clock.systemUTC()
  // 我们选择 HMAC SHA256 作为加密算法
  private val algorithm = JwtAlgorithm.HS256

  override def generateToken(sub: String, payload: Payload, audience: String, platform: Option[String] = None): AppTask[String] = ZIO.succeed:
    val expiration = platform match
      case Some("ios") | Some("android") =>
        Some(clock.instant().plusSeconds(30.days.toSeconds).getEpochSecond)
      case _ =>
        Some(clock.instant().plusSeconds(config.auth.token.expiration.accessToken.toSeconds).getEpochSecond)
    val claim = JwtClaim(
      subject = Some(sub),
      content = payload.toJson,
      issuer = Some(config.auth.token.issuer),
      audience = Some(Set(audience)),
      expiration = expiration,
      issuedAt = Some(clock.instant().getEpochSecond)
    )
    JwtZIOJson.encode(claim, config.auth.token.secret, algorithm)

  override def decode(token: String): AppTask[JwtClaim] =
    ZIO
      .fromTry(JwtZIOJson.decode(token, config.auth.token.secret, Seq(algorithm)))
      .mapError(ex => InvalidInput("jwt", s"Invalid token: ${ex.getMessage}"))

  override def validateToken(token: String): AppTask[String] =
    for
      claim <- decode(token)
      now = clock.instant().getEpochSecond
      _ <- ZIO.when(claim.expiration.exists(_ < now))(
        ZIO.fail(
          TokenExpired(
            context = Map("expiration" -> claim.expiration.toString)
          )
        )
      )
      sub <- ZIO
        .fromOption(claim.subject)
        .mapError(_ =>
          InvalidInput("sub", "Token is missing 'sub' field")
        )
    yield sub

object JwtServiceLive:
  val live: ZLayer[AppConfig, Nothing, JwtService] =
    ZLayer.fromFunction(new JwtServiceLive(_))
