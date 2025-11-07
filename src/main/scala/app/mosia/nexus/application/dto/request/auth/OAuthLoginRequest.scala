package app.mosia.nexus.application.dto.request.auth

import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class OAuthLoginRequest(
  provider: String,
  @jsonField("return_url") returnUrl: String,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
