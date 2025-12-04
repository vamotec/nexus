package app.mosia.nexus
package domain.model.resource

import domain.model.common.ValueObject

import zio.json.*

case class ControlEndpoint(wsUrl: String) extends ValueObject derives JsonCodec
