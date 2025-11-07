package app.mosia.nexus.application.service.auth

import app.mosia.nexus.application.dto.request.auth.BiometricAuthRequest
import app.mosia.nexus.application.dto.response.auth.{LoginResponse, RefreshTokenResponse}
import app.mosia.nexus.application.dto.response.user.UserResponse
import app.mosia.nexus.application.service.user.UserService
import app.mosia.nexus.application.util.CryptoUtils
import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.{ChallengeRow, GitHubUserInfo, RefreshToken, User, UserId}
import app.mosia.nexus.domain.repository.{AuthenticatorRepository, ChallengeRepository, RefreshTokenRepository}
import app.mosia.nexus.infra.auth.JwtService
import app.mosia.nexus.infra.config.AuthConfig
import app.mosia.nexus.infra.error.*
import org.mindrot.jbcrypt.BCrypt
import sttp.model.headers.Cookie.SameSite
import sttp.model.headers.CookieWithMeta
import zio.*
import zio.json.*

import java.time.Instant
import java.util.UUID
import java.util.Base64

// AuthServiceLive 的构造函数现在需要传入 userRepo 和 jwtService
final class AuthServiceLive(
  config: AuthConfig,
  userService: UserService,
  jwtService: JwtService,
  challengeRepo: ChallengeRepository,
  authenticatorRepo: AuthenticatorRepository,
  refreshTokenRepo: RefreshTokenRepository,
) extends AuthService:

  private val CHALLENGE_TTL_SECONDS = 90L

  override def createChallenge(userId: Option[UserId], deviceId: Option[DeviceId]): AppTask[ChallengeRow] =
    challengeRepo.createChallenge(userId, deviceId, purpose = "webauthn_auth", ttlSeconds = CHALLENGE_TTL_SECONDS)

  override def passwordLogin(email: String, plainPassword: String): AppTask[User] =
    for
      userOpt <- userService.authenticate(email, plainPassword)
      user <- ZIO.fromOption(userOpt).mapError(_ => InvalidCredentials)
    yield user

  def biometricLogin(bioRequest: BiometricAuthRequest): AppTask[User] =
    (for
      challengeOpt <- challengeRepo.findValidChallenge(bioRequest.challenge)
      challenge <- ZIO
        .fromOption(challengeOpt)
        .orElseFail(InvalidChallenge("Invalid or Expired challenge"))
      _ <- ZIO.when(challenge.deviceId.exists(_ != bioRequest.deviceId))(ZIO.fail(InvalidChallenge("Device mismatch")))
      deviceId <- DeviceId.fromStringZIO(bioRequest.deviceId)
      maybeAuth <- authenticatorRepo.findByDeviceIdAndKeyId(deviceId, bioRequest.keyId)
      auth <- ZIO.fromOption(maybeAuth).orElseFail(InvalidChallenge("Unknow deviceId or keyId"))
      // 4. verify signature
      // Data to verify: here we use the raw challenge bytes. If the client signed clientData or CBOR, adapt accordingly.
      verified <- ZIO.attempt:
        val data = bioRequest.challenge.getBytes("UTF-8")
        CryptoUtils.verifyEcdsaSha256(auth.publicKey, data, bioRequest.signature)
      _ <- if !verified then ZIO.fail(InvalidChallenge("Invalid signature")) else ZIO.unit
      // 5. optional: check signCount to detect cloned keys, update signCount and lastUsed
      // (if client provides signCount you would compare; for iOS simple flows, you might just update lastUsed)
      _ <- authenticatorRepo.setLastUsed(auth.id, Instant.now)
      // 6. consume challenge (atomicish)
      _ <- challengeRepo
        .consumeChallenge(bioRequest.challenge)
        .flatMap:
          case Some(_) => ZIO.unit
          case None => ZIO.fail(InvalidChallenge("Failed to consume challenge"))
      userOpt <- userService.findById(auth.userId)
      user <- ZIO.fromOption(userOpt).mapError(_ => InvalidCredentials)
    yield user).mapError(ErrorMapper.toAppError)

  // 生成短期 JWT Access Token
  override def generateAccessToken(userId: UserId, platform: Option[String]): AppTask[String] =
    jwtService.generateToken(userId, platform)

  // 生成长期 Refresh Token（随机字符串 + 数据库存储）
  override def generateRefreshToken(userId: UserId, platform: Option[String]): AppTask[String] =
    for
      // 生成安全的随机 token
      randomBytes <- Random.nextBytes(32)
      token = Base64.getUrlEncoder.withoutPadding().encodeToString(randomBytes.toArray)

      expiresAt = platform match
        case Some("ios") | Some("android") =>
          Instant.now().plusSeconds(90.days.toSeconds)
        case _ =>
          Instant.now().plusSeconds(config.token.expiration.refreshToken.toSeconds)
      // 存储到数据库
      record = RefreshToken(
        id = UUID.randomUUID(),
        token = token,
        userId = userId,
        expiresAt = expiresAt
      )
      _ <- refreshTokenRepo.save(record)
    yield token

  // 验证 Refresh Token
  override def validateRefreshToken(token: String): AppTask[UserId] =
    for
      recordOpt <- refreshTokenRepo.findByToken(token)
      record <- ZIO.fromOption(recordOpt).orElseFail(InvalidToken("Invalid refresh token"))
      _ <- ZIO
        .fail(InvalidToken("Token expired"))
        .when(record.expiresAt.isBefore(Instant.now()))
      _ <- ZIO
        .fail(InvalidToken("Token revoked"))
        .when(record.isRevoked)
    yield record.userId

  override def rotateRefreshToken(
    userId: UserId,
    oldToken: String,
    newToken: String,
    ttlSeconds: Long = 7 * 24 * 3600
  ): AppTask[Unit] =
    for
      maybeOld <- refreshTokenRepo.findByToken(oldToken)
      old <- ZIO
        .fromOption(maybeOld)
        .orElseFail(InvalidToken("Old refresh token not found"))
      _ <- if old.userId != userId then ZIO.fail(UserNotFound) else ZIO.unit
      _ <- refreshTokenRepo.markAsRevoked(oldToken) // 废弃旧 token
      newRow = RefreshToken(UUID.randomUUID(), newToken, userId, Instant.now().plusSeconds(ttlSeconds))
      _ <- refreshTokenRepo.save(newRow)
    yield ()

  override def buildAuthCookies(accessToken: String, refreshToken: String): AppTask[List[CookieWithMeta]] =
    ZIO.succeed:
      List(
        CookieWithMeta.unsafeApply(
          name = "access_token",
          value = accessToken,
          httpOnly = true,
          secure = true,
          path = Some("/"),
          sameSite = Some(SameSite.Strict),
          maxAge = Some(config.token.expiration.accessToken.toSeconds)
        ),
        CookieWithMeta.unsafeApply(
          name = "refresh_token",
          value = refreshToken,
          httpOnly = true,
          secure = true,
          path = Some("/api/refresh"),
          sameSite = Some(SameSite.Strict),
          maxAge = Some(config.token.expiration.refreshToken.toSeconds)
        )
      )

  override def buildLoginResponse(accessToken: String, refreshToken: String, user: User): AppTask[LoginResponse] =
    ZIO.succeed:
      LoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = config.token.expiration.accessToken.toSeconds,
        user = UserResponse(
          id = user.id.value.toString,
          username = user.name,
          email = user.email,
          avatar = Some(user.avatar),
          role = user.role.toString,
          createdAt = user.createdAt.toString
        )
      )

  override def buildRefreshTokenResponse(accessToken: String, refreshToken: String): AppTask[RefreshTokenResponse] =
    ZIO.succeed:
      RefreshTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken, // 返回新的 refresh token
        expiresIn = config.token.expiration.accessToken.toSeconds,
      )

  override def revokeRefreshToken(token: String): AppTask[Unit] = refreshTokenRepo.markAsRevoked(token)

object AuthServiceLive:
  // AuthService 的 ZIO Layer 现在同时依赖 UserRepository 和 JwtService
  val live: ZLayer[
    AuthConfig & UserService & JwtService & ChallengeRepository & AuthenticatorRepository & RefreshTokenRepository,
    Nothing,
    AuthServiceLive
  ] =
    ZLayer.fromFunction(new AuthServiceLive(_, _, _, _, _, _))
