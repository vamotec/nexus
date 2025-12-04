package app.mosia.nexus
package domain.config.cloud

final case class GrpcConnectionConfig(
  neuro: GrpcEndpoint,
  nebula: GrpcEndpoint,
  limits: GrpcLimits = GrpcLimits(),
  keepAlive: GrpcKeepAlive = GrpcKeepAlive(),
  transport: GrpcTransport = GrpcTransport()
)
