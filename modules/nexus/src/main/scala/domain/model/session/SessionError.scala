package app.mosia.nexus
package domain.model.session

import domain.model.common.ValueObject

import zio.json.*


case class SessionError(
  code: String,
  message: String
) extends ValueObject derives JsonCodec
