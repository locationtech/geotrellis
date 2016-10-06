package geotrellis.spark

import geotrellis.util._

import java.time.ZonedDateTime
import scala.util.{Failure, Success}

package object util {
  implicit class TryOption[T](option: Option[T]) {
    def mapNone(exception: => Throwable) = option match {
      case Some(t) => Success(t)
      case None    => Failure(exception)
    }
  }

  implicit class withLayerIdUtilMethods(val self: LayerId) extends MethodExtensions[LayerId] {
    def createTemporaryId(): LayerId = self.copy(name = s"${self.name}-${ZonedDateTime.now.toInstant.toEpochMilli}")
  }
}
