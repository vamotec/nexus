package app.mosia.nexus
package domain.model.common

import zio.json.JsonCodec

case class StringTimeRange(start: String, end: String) derives JsonCodec
