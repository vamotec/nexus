package app.mosia.nexus
package application.dto.request.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class RefreshTokenRequest(
  @jsonField("refresh_token") refreshToken: String,
  @jsonField("device_info") deviceInfo: Option[DeviceInfo] = None
) derives JsonCodec,
      Schema
