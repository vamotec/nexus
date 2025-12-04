package app.mosia.nexus
package domain.model.organization

import domain.model.common.ValueObject

import zio.json.*

enum OrganizationRole extends ValueObject derives JsonCodec:
  case Owner, Admin, Member

object OrganizationRole:
  def fromString(s: String): OrganizationRole = s.toLowerCase match
    case "owner" => OrganizationRole.Owner
    case "admin" => OrganizationRole.Admin
    case "member" => OrganizationRole.Member
    case _ => throw new IllegalArgumentException(s"Unknown organization role: $s")
