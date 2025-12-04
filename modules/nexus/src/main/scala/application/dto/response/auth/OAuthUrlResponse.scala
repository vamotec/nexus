package app.mosia.nexus
package application.dto.response.auth

import sttp.tapir.Schema
import zio.json.{JsonCodec, SnakeCase, jsonMemberNames}

@jsonMemberNames(SnakeCase)
case class OAuthUrlResponse(
                            authUrl: String,
                            state: String,
                          ) derives JsonCodec, Schema
