package app.mosia.nexus
package domain.event

import domain.model.resource.ResourceAssignment
import domain.model.session.SessionId
import domain.model.user.UserId

import zio.json.*

import java.time.Instant
import java.util.UUID

@jsonDiscriminator("type")
enum SessionEvent derives JsonCodec:
  case SessionCreated(sessionId: SessionId, userId: UserId, occurredAt: Instant)
  case SessionStarted(sessionId: SessionId, assignment: ResourceAssignment, occurredAt: Instant)
  case SessionStopped(sessionId: SessionId, reason: String, occurredAt: Instant)

  def aggregateId: UUID = this match
    case SessionCreated(sessionId, _, _) => sessionId.value
    case SessionStarted(sessionId, _, _) => sessionId.value
    case SessionStopped(sessionId, _, _) => sessionId.value
