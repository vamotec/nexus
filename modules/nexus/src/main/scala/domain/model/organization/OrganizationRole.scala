package app.mosia.nexus
package domain.model.organization

import domain.model.common.ValueObject

import io.getquill.MappedEncoding
import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*

enum OrganizationRole extends ValueObject derives JsonCodec:
  case Owner, Admin, Member

object OrganizationRole:
  def fromString(s: String): OrganizationRole = s.toLowerCase match
    case "owner" => OrganizationRole.Owner
    case "admin" => OrganizationRole.Admin
    case "member" => OrganizationRole.Member
    case _ => throw new IllegalArgumentException(s"Unknown organization role: $s")

  given MappedEncoding[OrganizationRole, String] = MappedEncoding(_.toString.toLowerCase)
  given MappedEncoding[String, OrganizationRole] = MappedEncoding(fromString)

  given Cs[Any, OrganizationRole] = Cs.stringSchema.contramap(_.toString.toLowerCase)
  given ArgBuilder[OrganizationRole] = ArgBuilder.string.map(fromString)
  given Schema[OrganizationRole] = Schema.derivedEnumeration.defaultStringBased
