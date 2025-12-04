package app.mosia.nexus
package infrastructure.cloud

import domain.config.AppConfig
import domain.error.*
import domain.grpc.nebula.*
import domain.grpc.nebula.{ListFilesRequest, ListFilesResponse}
import domain.model.grpc.ClusterTarget.Nebula
import domain.services.infra.{ClusterRegistry, NebulaClient}

import zio.{ZIO, ZLayer}

private final class NebulaClientLive(
                            manager: ConnectionManager,
                            clusterRegistry: ClusterRegistry
                          ) extends NebulaClient:

  private def fileServiceRouting[A](
                                    clusterId: String,
                                    f: NebulaFileServiceGrpc.NebulaFileServiceStub => scala.concurrent.Future[A]
                                  ): AppTask[A] =
    for {
      _ <- clusterRegistry.updateLoad(clusterId, +1)
      stub <- manager.getNebulaFileStub(clusterId)
      res <- ZIO
        .fromFuture(_ => f(stub))
        .mapError(toAppError)
        .ensuring(clusterRegistry.updateLoad(clusterId, -1))
    } yield res

  private def collabServiceRouting[A](
                                     clusterId: String,
                                     f: NebulaCollabServiceGrpc.NebulaCollabServiceStub => scala.concurrent.Future[A]
                                   ): AppTask[A] =
    for {
      _ <- clusterRegistry.updateLoad(clusterId, +1)
      stub <- manager.getNebulaCollabStub(clusterId)
      res <- ZIO
        .fromFuture(_ => f(stub))
        .mapError(toAppError)
        .ensuring(clusterRegistry.updateLoad(clusterId, -1))
    } yield res

  private def syncServiceRouting[A](
                                     clusterId: String,
                                     f: NebulaSyncServiceGrpc.NebulaSyncServiceStub => scala.concurrent.Future[A]
                                   ): AppTask[A] =
    for {
      _ <- clusterRegistry.updateLoad(clusterId, +1)
      stub <- manager.getNebulaSyncStub(clusterId)
      res <- ZIO
        .fromFuture(_ => f(stub))
        .mapError(toAppError)
        .ensuring(clusterRegistry.updateLoad(clusterId, -1))
    } yield res

  private def nucleusServiceRouting[A](
                                     clusterId: String,
                                     f: NebulaNucleusServiceGrpc.NebulaNucleusServiceStub => scala.concurrent.Future[A]
                                   ): AppTask[A] =
    for {
      _ <- clusterRegistry.updateLoad(clusterId, +1)
      stub <- manager.getNebulaNucleusStub(clusterId)
      res <- ZIO
        .fromFuture(_ => f(stub))
        .mapError(toAppError)
        .ensuring(clusterRegistry.updateLoad(clusterId, -1))
    } yield res
    
  
  override def listFiles(clusterId: String, request: ListFilesRequest): AppTask[ListFilesResponse] =
    fileServiceRouting(clusterId, _.listFiles(request))
  
object NebulaClientLive:
  val live: ZLayer[ConnectionManager & ClusterRegistry, Nothing, NebulaClient] =
    ZLayer.scoped:
      for
        manager <- ZIO.service[ConnectionManager]
        registry <- ZIO.service[ClusterRegistry]
      yield NebulaClientLive(manager, registry)

  val layer: ZLayer[AppConfig & ClusterRegistry, AppError, NebulaClient] = ConnectionManager.make(Nebula) >>> live
