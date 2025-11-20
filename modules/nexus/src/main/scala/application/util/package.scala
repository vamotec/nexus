package app.mosia.nexus
package application

import zio.json.*
import zio.*

package object util:
  extension [A](opt: Option[A])
    def toZIOOrFail[E](error: => E): IO[E, A] =
      ZIO.fromOption(opt).mapError(_ => error)
