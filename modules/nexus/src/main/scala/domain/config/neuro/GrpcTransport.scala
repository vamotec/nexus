package app.mosia.nexus
package domain.config.neuro

import zio.*

final case class GrpcTransport(
  usePlaintext: Boolean = true,
  userAgent: Option[String] = None,
  loadBalancingPolicy: String = "round_robin",
  idleTimeout: Duration = 30.minutes,
  connectTimeout: Duration = 10.seconds
)
