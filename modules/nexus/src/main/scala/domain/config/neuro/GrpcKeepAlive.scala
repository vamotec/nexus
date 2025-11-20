package app.mosia.nexus
package domain.config.neuro

import zio.*

final case class GrpcKeepAlive(
  time: Duration = 30.seconds,
  timeout: Duration = 10.seconds,
  withoutCalls: Boolean = true
)
