package app.mosia.nexus
package application.dto.request.auth

import sttp.tapir.Schema
import zio.json.*

case class AuthRequest(
  email: Option[String],
  password: Option[String],
  name: Option[String],
  @jsonField("biometric_token") biometric: Option[BiometricAuthRequest] = None,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
