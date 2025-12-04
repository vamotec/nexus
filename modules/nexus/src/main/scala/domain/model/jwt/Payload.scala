package app.mosia.nexus
package domain.model.jwt

import domain.model.organization.OrganizationId
import domain.model.project.ProjectId
import domain.model.session.SessionId
import domain.model.user.UserId

import zio.json.{JsonCodec, SnakeCase, jsonMemberNames}

@jsonMemberNames(SnakeCase)
case class Payload(
  sessionId: Option[String] = None,
  orgId: Option[String] = None,
  projectIds: Option[Set[String]] = None,
  permission: Set[Permission],
  tokenType: TokenType
) derives JsonCodec
