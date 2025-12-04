package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.*
import zio.json.*

@jsonMemberNames(SnakeCase)
case class ProviderTokenResponse(
  accessToken: String,
  tokenType: String,
  expiresIn: Option[Int] = None,
  refreshToken: Option[String] = None,
  scope: Option[String] = None
) derives JsonCodec,
      Schema
