package app.mosia.nexus.application.dto.request.auth

import sttp.tapir.Schema
import zio.json.{jsonField, JsonCodec}

case class LoginRequest(
  email: Option[String],
  password: Option[String],
  @jsonField("biometric_token") biometric: Option[BiometricAuthRequest] = None,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
