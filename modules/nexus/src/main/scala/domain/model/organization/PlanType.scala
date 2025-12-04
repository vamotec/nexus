package app.mosia.nexus
package domain.model.organization

import domain.model.common.ValueObject

import caliban.schema.{ArgBuilder, Schema as Cs}
import sttp.tapir.Schema
import zio.json.*

enum PlanType extends ValueObject derives JsonCodec, Schema, Cs.SemiAuto, ArgBuilder:
  case Free, Premium, Enterprise

object PlanType:
  def fromString(s: String): PlanType = s.toLowerCase match
    case "free" => PlanType.Free
    case "premium" => PlanType.Premium
    case "enterprise" => PlanType.Enterprise
    case _ => throw new IllegalArgumentException(s"Unknown plan type: $s")
