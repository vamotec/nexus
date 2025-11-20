package app.mosia.nexus
package domain.services.app

import application.dto.response.auth.CallbackResponse
import domain.error.{AppError, AppTask}
import domain.model.user.User

import zio.json.*
import zio.*
import zio.http.*

trait OAuth2Service:
  def getAuthorizationUrl(provider: String, returnUrl: String, platform: Option[String]): AppTask[URL]
  def handleCallback(provider: String, code: String, stateId: String): ZIO[Scope, AppError, CallbackResponse]
  def getUserInfo(provider: String, accessToken: String): ZIO[Scope, AppError, String]
  def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String]
  def isValidReturnUrl(url: String): AppTask[Boolean]
  def oauthLogin(provider: String, userInfoJson: String): AppTask[User]
  def buildRedirectUrl(accessToken: String, refreshToken: String, platform: Option[String]): AppTask[String]
