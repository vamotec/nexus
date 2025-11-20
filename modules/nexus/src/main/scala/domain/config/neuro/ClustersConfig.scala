package app.mosia.nexus
package domain.config.neuro

case class ClustersConfig(
  grpc: GrpcPoolConfig,
  websocket: WebSocketConfig,
  location: GeoLocation,
  capacity: Int = 100,
  priority: Int = 0,
  tags: Set[String] = Set.empty
)
