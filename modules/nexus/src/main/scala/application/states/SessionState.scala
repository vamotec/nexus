package app.mosia.nexus
package application.states

import zio.json.*
import zio.http.*
import zio.*

case class SessionState(
  frontend: Option[WebSocketChannel],
  isaacSim: Option[WebSocketChannel]
):
  def isReady: Boolean = frontend.isDefined && isaacSim.isDefined
