package geotrellis.spark

import scala.reflect._
import scala.util.{Failure, Success, Try}

package object io {
  implicit class TryOption[T](option: Option[T]) {
    def toTry(exception: => Throwable): Try[T] =
      option match {
        case Some(t) => Success(t)
        case None    => Failure(exception)
      }
  }

  // Custom exceptions
  class CatalogError(val message: String) extends Exception(message)

  class DriverNotFoundError[K: ClassTag]
      extends CatalogError(s"Driver not found for key type '${classTag[K]}'")

  class LayerNotFoundError(layerId: LayerId)
      extends CatalogError(s"LayerMetaData not found for layer $layerId")

  class MultipleMatchError(layerId: LayerId)
    extends CatalogError(s"Multiple layers match id of $layerId")

  class LayerExistsError(layerId: LayerId) 
      extends CatalogError(s"Layer ${layerId} already exists in the catalog")

}
