package app.mosia.nexus
package application.dto.request.organization

import sttp.tapir.Schema
import zio.json.*

case class InviteMemberRequest(
  userId: String, // UUID string
  role: String    // "admin" or "member"
) derives JsonCodec, Schema
