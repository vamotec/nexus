package app.mosia.nexus
package application.dto.request.organization

import sttp.tapir.Schema
import zio.json.*

case class UpdateOrganizationRequest(
  name: Option[String],
  description: Option[String],
  avatar: Option[String]
) derives JsonCodec, Schema
