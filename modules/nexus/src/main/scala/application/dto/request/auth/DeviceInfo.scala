package app.mosia.nexus
package application.dto.request.auth

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*
import zio.*
import zio.json.ast.Json

case class DeviceInfo(
  @jsonField("device_id") deviceId: String,
  @jsonField("device_name") deviceName: String,
  platform: String, // "ios" | "android" | "web"
  @jsonField("os_version") osVersion: String,
  @jsonField("app_version") appVersion: String,
  @jsonField("push_token") pushToken: Option[String] = None
) derives JsonCodec,
      Schema
