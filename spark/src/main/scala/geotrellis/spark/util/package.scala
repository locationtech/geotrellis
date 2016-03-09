package geotrellis.spark

import geotrellis.util._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

package object util {
  implicit class TryOption[T](option: Option[T]) {
    def mapNone(exception: => Throwable) = option match {
      case Some(t) => Success(t)
      case None    => Failure(exception)
    }
  }

  implicit class withLayerIdUtilMethods(val self: LayerId) extends MethodExtensions[LayerId] {
    def createTemporaryId(): LayerId = self.copy(name = s"${self.name}-${DateTime.now.getMillis}")
  }
}
