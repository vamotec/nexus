package app.mosia.nexus
package application.dto.request.auth

import sttp.tapir.Schema
import zio.json.*

case class OAuthLoginRequest(
  provider: String,
  @jsonField("return_url") returnUrl: String,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
