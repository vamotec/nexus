package app.mosia.nexus.application.dto.request.auth

import sttp.tapir.Schema
import zio.json.JsonCodec

case class BiometricAuthRequest(
  deviceId: String,
  keyId: String,
  challenge: String,
  signature: String, // base64 of signature
  clientData: Option[String] = None // optional extra data if you include it
) derives JsonCodec,
      Schema
