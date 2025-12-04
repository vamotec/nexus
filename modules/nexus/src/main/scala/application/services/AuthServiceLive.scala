package app.mosia.nexus
package application.services

import application.dto.request.auth.BiometricAuthRequest
import application.dto.response.auth.{LoginResponse, RefreshTokenResponse}
import application.dto.response.user.UserResponse
import application.util.CryptoUtils
import domain.config.AppConfig
import domain.error.*
import domain.model.jwt.TokenType.Access
import domain.model.jwt.{Payload, Permission}
import domain.model.user.*
import domain.repository.{AuthenticatorRepository, ChallengeRepository, RefreshTokenRepository}
import domain.services.app.{AuthService, UserService}
import domain.services.infra.JwtService

import app.mosia.nexus.domain.model.user.UserRole.toPermission
import sttp.model.headers.Cookie.SameSite
import sttp.model.headers.CookieWithMeta
import zio.*

import java.time.Instant
import java.util.Base64

// AuthServiceLive 的构造函数现在需要传入 userRepo 和 jwtService
final class AuthServiceLive(
  config: AppConfig,
  userService: UserService,
  jwtService: JwtService,
  challengeRepo: ChallengeRepository,
  authenticatorRepo: AuthenticatorRepository,
  refreshTokenRepo: RefreshTokenRepository
) extends AuthService:

  private val CHALLENGE_TTL_SECONDS = 90L

  override def createChallenge(userId: Option[UserId], deviceId: Option[String]): AppTask[Challenge] =
    challengeRepo.createChallenge(userId, deviceId, purpose = "webauthn_auth", ttlSeconds = CHALLENGE_TTL_SECONDS)

  override def passwordLogin(email: String, plainPassword: String): AppTask[User] =
    for
      userOpt <- userService.authenticate(email, plainPassword)
      user <- ZIO.fromOption(userOpt).mapError(_ => InvalidCredentials("login service"))
    yield user

  override def registerUser(email: String, plainPassword: String, name: String): AppTask[User] = 
    userService.createUser(email, plainPassword, Some(name))
      
  def biometricLogin(bioRequest: BiometricAuthRequest): AppTask[User] =
    for
      challengeOpt <- challengeRepo.findValidChallenge(bioRequest.challenge)
      challenge <- ZIO
        .fromOption(challengeOpt)
        .orElseFail(InvalidChallenge())
      _ <- ZIO.when(challenge.deviceId.exists(_ != bioRequest.deviceId))(ZIO.fail(Unauthenticated("Device mismatch")))
      maybeAuth <- authenticatorRepo.findByDeviceIdAndKeyId(bioRequest.deviceId, bioRequest.keyId)
      auth <- ZIO.fromOption(maybeAuth).orElseFail(Unauthenticated("Unknow deviceId or keyId"))
      // 4. verify signature
      // Data to verify: here we use the raw challenge bytes. If the client signed clientData or CBOR, adapt accordingly.
      verified <- ZIO
        .attempt:
          val data = bioRequest.challenge.getBytes("UTF-8")
          CryptoUtils.verifyEcdsaSha256(auth.publicKey, data, bioRequest.signature)
        .mapError(toAppError)
      _ <- if !verified then ZIO.fail(Unauthenticated("Invalid signature")) else ZIO.unit
      // 5. optional: check signCount to detect cloned keys, update signCount and lastUsed
      // (if client provides signCount you would compare; for iOS simple flows, you might just update lastUsed)
      _ <- authenticatorRepo.setLastUsed(auth.id, Instant.now)
      // 6. consume challenge (atomicish)
      _ <- challengeRepo
        .consumeChallenge(bioRequest.challenge)
        .flatMap:
          case Some(_) => ZIO.unit
          case None => ZIO.fail(Unauthenticated("Failed to consume challenge"))
      userOpt <- userService.findById(auth.userId.value.toString)
      user <- ZIO.fromOption(userOpt).mapError(_ => InvalidCredentials("biometric"))
    yield user

  // 生成短期 JWT Access Token
  override def generateAccessToken(userId: String, platform: Option[String]): AppTask[String] =
    for 
      userOpt <- userService.findById(userId)
      user <- ZIO.fromOption(userOpt)
        .orElseFail(NotFound("user", userId))
      permission = toPermission(user.role)
      payload = Payload(
        permission = Set(permission), tokenType = Access
      )
      token <- jwtService.generateToken(userId, payload, "nexus", platform)
    yield token

  // 生成长期 Refresh Token（随机字符串 + 数据库存储）
  override def generateRefreshToken(userId: String, platform: Option[String]): AppTask[String] =
    for
      // 生成安全的随机 token
      randomBytes <- Random.nextBytes(32)
      token = Base64.getUrlEncoder.withoutPadding().encodeToString(randomBytes.toArray)

      expiresAt = platform match
        case Some("ios") | Some("android") =>
          Instant.now().plusSeconds(90.days.toSeconds)
        case _ =>
          Instant.now().plusSeconds(config.auth.token.expiration.refreshToken.toSeconds)
      // 存储到数据库
      id <- Random.nextLong.map(_.abs)
      record = RefreshToken(
        id = id,
        token = token,
        userId = userId,
        expiresAt = expiresAt,
        isRevoked = None
      )
      _ <- refreshTokenRepo.save(record)
    yield token

  // 验证 Refresh Token
  override def validateRefreshToken(token: String): AppTask[String] =
    for
      recordOpt <- refreshTokenRepo.findByToken(token)
      record <- ZIO.fromOption(recordOpt).orElseFail(InvalidInput("refresh token", "Invalid refresh token"))
      _ <- ZIO
        .fail(TokenExpired("refresh token"))
        .when(record.expiresAt.isBefore(Instant.now()))
      _ <- ZIO
        .fail(InvalidInput("refresh token", "Token revoked"))
        .when(record.isRevoked.contains(true))
    yield record.userId

  override def rotateRefreshToken(
    userId: String,
    oldToken: String,
    newToken: String,
    ttlSeconds: Long = 7 * 24 * 3600
  ): AppTask[Unit] =
    for
      maybeOld <- refreshTokenRepo.findByToken(oldToken)
      old <- ZIO
        .fromOption(maybeOld)
        .orElseFail(InvalidInput("refresh token", "Old refresh token not found"))
      _ <- if old.userId != userId then ZIO.fail(NotFound("User", userId)) else ZIO.unit
      _ <- refreshTokenRepo.markAsRevoked(oldToken) // 废弃旧 token
      id <- Random.nextLong.map(_.abs)
      newRow = RefreshToken(id, newToken, userId, Instant.now().plusSeconds(ttlSeconds), None)
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
          maxAge = Some(config.auth.token.expiration.accessToken.toSeconds)
        ),
        CookieWithMeta.unsafeApply(
          name = "refresh_token",
          value = refreshToken,
          httpOnly = true,
          secure = true,
          path = Some("/api/refresh"),
          sameSite = Some(SameSite.Strict),
          maxAge = Some(config.auth.token.expiration.refreshToken.toSeconds)
        )
      )

  override def buildLoginResponse(accessToken: String, refreshToken: String, user: User): AppTask[LoginResponse] =
    ZIO.succeed:
      LoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = config.auth.token.expiration.accessToken.toSeconds,
        user = UserResponse(
          id = user.id.value.toString,
          username = user.name,
          email = user.email,
          avatar = user.avatar,
          role = user.role.toString,
          createdAt = user.createdAt.toString
        )
      )

  override def buildRefreshTokenResponse(accessToken: String, refreshToken: String): AppTask[RefreshTokenResponse] =
    ZIO.succeed:
      RefreshTokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken, // 返回新的 refresh token
        expiresIn = config.auth.token.expiration.accessToken.toSeconds
      )

  override def revokeRefreshToken(token: String): AppTask[Unit] = refreshTokenRepo.markAsRevoked(token)

object AuthServiceLive:
  // AuthService 的 ZIO Layer 现在同时依赖 UserRepository 和 JwtService
  val live: ZLayer[
    AppConfig & UserService & JwtService & ChallengeRepository & AuthenticatorRepository & RefreshTokenRepository,
    Nothing,
    AuthServiceLive
  ] =
    ZLayer.fromFunction(new AuthServiceLive(_, _, _, _, _, _))
