package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.model.grpc.RoutingContext

trait ClusterRoutingStrategy:
  def selectCluster(context: RoutingContext): AppTask[String]
