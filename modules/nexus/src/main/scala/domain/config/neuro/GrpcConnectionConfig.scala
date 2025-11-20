package app.mosia.nexus
package domain.config.neuro

final case class GrpcConnectionConfig(
  endpoint: GrpcEndpoint,
  limits: GrpcLimits = GrpcLimits(),
  keepAlive: GrpcKeepAlive = GrpcKeepAlive(),
  transport: GrpcTransport = GrpcTransport()
)
