package app.mosia.nexus.domain.model.resource

import zio.json.JsonCodec

case class IsaacSimInstanceId(value: String) derives JsonCodec
