package app.mosia.nexus.application.dto.response.auth

import sttp.tapir.Schema
import zio.json.JsonCodec

case class OAuthResponse(
  jwt: String,
  returnUrl: String
) derives JsonCodec,
      Schema
