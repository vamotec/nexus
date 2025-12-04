package app.mosia.nexus
package domain.model.user

import java.time.Instant
import java.util.UUID

case class OAuthProvider(
                          id: UUID,
                          userId: UserId,
                          provider: Provider,
                          providerUserId: String,
                          providerEmail: Option[String],
                          linkedAt: Instant,
                          lastUsedAt: Option[Instant]
                        )
