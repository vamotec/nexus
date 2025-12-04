package app.mosia.nexus
package domain.model.organization

import domain.model.common.ValueObject

import sttp.tapir.Schema
import zio.json.*

enum PlanType extends ValueObject derives JsonCodec, Schema:
  case Free, Premium, Enterprise

object PlanType:
  def fromString(s: String): PlanType = s.toLowerCase match
    case "free" => PlanType.Free
    case "premium" => PlanType.Premium
    case "enterprise" => PlanType.Enterprise
    case _ => throw new IllegalArgumentException(s"Unknown plan type: $s")
