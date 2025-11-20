package app.mosia.nexus
package domain.model.grpc

final case class PoolHealth(total: Int, healthy: Int, unhealthy: Int)
