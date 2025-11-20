package app.mosia.nexus
package domain.model.grpc

import domain.model.common.Priority
import domain.model.session.SessionMode
import domain.config.neuro.GeoLocation
import domain.model.project.*
import domain.model.user.UserId

// 2. 路由上下文
final case class RoutingContext(
  userId: UserId,
  projectId: ProjectId,
  project: Project,
  userLocation: GeoLocation,
  sessionMode: SessionMode = SessionMode.Manual,
  priority: Priority = Priority.Normal
)
