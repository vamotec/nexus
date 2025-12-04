package app.mosia.nexus
package application.dto.response.organization

import domain.model.organization.Organization

import sttp.tapir.Schema
import zio.json.JsonCodec

case class OrganizationsResponse (id: String, name: String, planType: String) derives JsonCodec, Schema
object OrganizationsResponse:
  def fromDomain(org: Organization): OrganizationsResponse =
    OrganizationsResponse(
      id = org.id.value.toString,
      name = org.name,
      planType = org.planType.toString.toLowerCase
    )