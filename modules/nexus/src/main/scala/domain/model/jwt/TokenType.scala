package app.mosia.nexus
package domain.model.jwt

import zio.json.JsonCodec

enum TokenType derives JsonCodec:
  case Access, Refresh, Control, Nebula
