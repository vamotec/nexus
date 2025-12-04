package app.mosia.nexus
package domain.config.cloud

final case class GrpcLimits(
  maxInboundMessageSize: Int = 4 * 1024 * 1024,
  maxInboundMetadataSize: Int = 8 * 1024,
  flowControlWindow: Int = 1048576
)
