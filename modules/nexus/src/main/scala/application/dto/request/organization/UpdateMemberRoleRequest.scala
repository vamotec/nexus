package app.mosia.nexus
package application.dto.request.organization

import sttp.tapir.Schema
import zio.json.*

case class UpdateMemberRoleRequest(
  role: String // "admin" or "member"
) derives JsonCodec, Schema
