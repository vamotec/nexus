package app.mosia.nexus
package domain.model.scene

import domain.model.common.ValueObject

import zio.json.*

enum EnvironmentType extends ValueObject derives JsonCodec:
  case Warehouse, Factory, Laboratory, Outdoor
