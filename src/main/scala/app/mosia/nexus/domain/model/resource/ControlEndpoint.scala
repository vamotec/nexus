package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.common.ValueObject
import zio.json.JsonCodec

case class ControlEndpoint(wsUrl: String) extends ValueObject derives JsonCodec
