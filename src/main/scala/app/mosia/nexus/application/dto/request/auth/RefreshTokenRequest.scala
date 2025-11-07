package app.mosia.nexus.application.dto.request.auth

import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class RefreshTokenRequest(
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
