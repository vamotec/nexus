package app.mosia.nexus.application.service.auth

import app.mosia.nexus.application.dto.request.auth.BiometricAuthRequest
import app.mosia.nexus.application.dto.response.auth.{LoginResponse, RefreshTokenResponse}
import app.mosia.nexus.domain.model.device.DeviceId
import app.mosia.nexus.domain.model.user.{ChallengeRow, User, UserId}
import app.mosia.nexus.infra.error.AppTask
import sttp.model.headers.CookieWithMeta
import zio.*

trait AuthService:
  def passwordLogin(email: String, plainPassword: String): AppTask[User]

  def biometricLogin(bioRequest: BiometricAuthRequest): AppTask[User]

  def createChallenge(userId: Option[UserId], deviceId: Option[DeviceId]): AppTask[ChallengeRow]

  def generateAccessToken(userId: UserId, platform: Option[String]): AppTask[String]

  def generateRefreshToken(userId: UserId, platform: Option[String]): AppTask[String]

  def validateRefreshToken(token: String): AppTask[UserId]

  def revokeRefreshToken(token: String): AppTask[Unit]

  def rotateRefreshToken(
    userId: UserId,
    oldToken: String,
    newToken: String,
    ttlSeconds: Long = 7 * 24 * 3600
  ): AppTask[Unit]

  def buildAuthCookies(accessToken: String, refreshToken: String): AppTask[List[CookieWithMeta]]

  def buildLoginResponse(accessToken: String, refreshToken: String, user: User): AppTask[LoginResponse]

  def buildRefreshTokenResponse(accessToken: String, refreshToken: String): AppTask[RefreshTokenResponse]
