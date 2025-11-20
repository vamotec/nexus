package app.mosia.nexus
package application.dto.request.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class BiometricAuthRequest(
  deviceId: String,
  keyId: String,
  challenge: String,
  signature: String, // base64 of signature
  clientData: Option[String] = None // optional extra data if you include it
) derives JsonCodec,
      Schema
