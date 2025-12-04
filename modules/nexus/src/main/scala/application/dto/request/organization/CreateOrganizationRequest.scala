package app.mosia.nexus
package application.dto.request.organization

import sttp.tapir.Schema
import zio.json.*

case class CreateOrganizationRequest(
  name: String,
  description: Option[String]
) derives JsonCodec, Schema
