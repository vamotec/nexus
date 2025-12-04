package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask
import domain.grpc.nebula.*

trait NebulaClient:
  def listFiles(clusterId: String, request: ListFilesRequest): AppTask[ListFilesResponse]
