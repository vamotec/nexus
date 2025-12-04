package app.mosia.nexus
package domain.model.jwt

import zio.json.JsonCodec

enum Permission derives JsonCodec:
  case Viewer, Editor, Admin
