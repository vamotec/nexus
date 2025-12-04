package app.mosia.nexus
package domain.config.auth

case class OAuth2ClientConfig(
  mode: String,
  clientId: String,
  clientSecret: String,
  authorizationUrl: String,
  accessTokenUrl: String,
  userInfoUrl: String,
  mockUrl: String,
  scope: String
):
  def isMockMode: Boolean = mode == "mock"
  
  def authUrl: String =
    if (isMockMode) s"${mockUrl}/login/oauth/authorize"
    else s"$authorizationUrl"

  def tokenUrl: String =
    if (isMockMode) s"${mockUrl}/login/oauth/access_token"
    else s"$accessTokenUrl"

  def userUrl: String =
    if (isMockMode) s"${mockUrl}/user"
    else s"$userInfoUrl"

  def getClientId: String =
    if (isMockMode) "mock_client_id"
    else clientId

  def getClientSecret: String =
    if (isMockMode) "mock_client_secret"
    else clientSecret
