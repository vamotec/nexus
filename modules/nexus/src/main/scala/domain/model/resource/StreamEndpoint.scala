package app.mosia.nexus
package domain.model.resource

import domain.model.common.ValueObject

import zio.json.*

case class StreamEndpoint(host: String, port: Int, protocol: String) extends ValueObject derives JsonCodec
