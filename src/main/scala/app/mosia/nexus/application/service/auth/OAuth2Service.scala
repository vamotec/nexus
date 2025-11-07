package app.mosia.nexus.application.service.auth

import app.mosia.nexus.application.dto.response.auth.{CallbackResponse, OAuthResponse}
import app.mosia.nexus.domain.model.user.User
import app.mosia.nexus.infra.error.{AppError, AppTask}
import zio.*
import zio.http.URL

trait OAuth2Service:
  def getAuthorizationUrl(provider: String, returnUrl: String, platform: Option[String]): AppTask[URL]
  def handleCallback(provider: String, code: String, stateId: String): ZIO[Scope, AppError, CallbackResponse]
  def getUserInfo(provider: String, accessToken: String): ZIO[Scope, AppError, String]
  def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String]
  def isValidReturnUrl(url: String): AppTask[Boolean]
  def oauthLogin(provider: String, userInfoJson: String): AppTask[User]
  def buildRedirectUrl(accessToken: String, refreshToken: String, platform: Option[String]): AppTask[String]
