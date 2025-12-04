package app.mosia.nexus
package domain.services.app

import application.dto.response.auth.CallbackResponse
import domain.error.{AppError, AppTask}
import domain.model.user.{Provider, User}

import zio.*
import zio.http.*

trait OAuth2Service:
  def getAuthorizationUrl(provider: String, returnUrl: String, platform: Option[String]): AppTask[(URL, String)]
  def handleCallback(provider: String, code: String, stateId: String): AppTask[CallbackResponse]
  def getUserInfo(provider: Provider, accessToken: String): AppTask[String]
  def extractAndValidateReturnUrl(queryParam: Option[String]): AppTask[String]
  def isValidReturnUrl(url: String): AppTask[Boolean]
  def oauthLogin(provider: Provider, userInfoJson: String): AppTask[User]
