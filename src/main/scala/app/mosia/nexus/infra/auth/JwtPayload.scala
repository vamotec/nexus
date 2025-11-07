package app.mosia.nexus.infra.auth

import zio.json.{jsonMemberNames, JsonCodec, SnakeCase}

import java.util.UUID

@jsonMemberNames(SnakeCase)
case class JwtPayload(userIdStr: String) derives JsonCodec
