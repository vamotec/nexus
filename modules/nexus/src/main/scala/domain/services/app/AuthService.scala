package app.mosia.nexus
package domain.services.app

import application.dto.request.auth.BiometricAuthRequest
import application.dto.response.auth.{LoginResponse, RefreshTokenResponse}
import domain.error.AppTask
import domain.model.user.{Challenge, User, UserId}

import sttp.model.headers.CookieWithMeta

trait AuthService:
  def passwordLogin(email: String, plainPassword: String): AppTask[User]

  def registerUser(email: String, plainPassword: String, name: String): AppTask[User]

  def biometricLogin(bioRequest: BiometricAuthRequest): AppTask[User]

  def createChallenge(userId: Option[UserId], deviceId: Option[String]): AppTask[Challenge]

  def generateAccessToken(userId: String, platform: Option[String]): AppTask[String]

  def generateRefreshToken(userId: String, platform: Option[String]): AppTask[String]

  def validateRefreshToken(token: String): AppTask[String]

  def revokeRefreshToken(token: String): AppTask[Unit]

  def rotateRefreshToken(
    userId: String,
    oldToken: String,
    newToken: String,
    ttlSeconds: Long = 7 * 24 * 3600
  ): AppTask[Unit]

  def buildAuthCookies(accessToken: String, refreshToken: String): AppTask[List[CookieWithMeta]]

  def buildLoginResponse(accessToken: String, refreshToken: String, user: User): AppTask[LoginResponse]

  def buildRefreshTokenResponse(accessToken: String, refreshToken: String): AppTask[RefreshTokenResponse]
