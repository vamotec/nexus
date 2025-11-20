package app.mosia.nexus
package domain.model.common

import java.time.Instant

import caliban.schema.{ArgBuilder, Schema as Cs}

case class AbsoluteTimeRange(start: Instant, end: Instant) derives Cs.SemiAuto, ArgBuilder
