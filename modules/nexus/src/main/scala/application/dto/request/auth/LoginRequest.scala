package app.mosia.nexus
package application.dto.request.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class LoginRequest(
  email: Option[String],
  password: Option[String],
  @jsonField("biometric_token") biometric: Option[BiometricAuthRequest] = None,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
