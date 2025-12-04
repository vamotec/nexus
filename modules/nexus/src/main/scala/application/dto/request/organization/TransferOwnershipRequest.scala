package app.mosia.nexus
package application.dto.request.organization

import sttp.tapir.Schema
import zio.json.*

case class TransferOwnershipRequest(
  newOwnerId: String // UUID string
) derives JsonCodec, Schema
