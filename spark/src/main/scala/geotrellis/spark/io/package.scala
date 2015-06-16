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

  class LayerNotFoundError(layerId: LayerId)
      extends CatalogError(s"LayerMetaData not found for layer $layerId")

  class LayerExistsError(layerId: LayerId)
      extends CatalogError(s"Layer ${layerId} already exists in the catalog")

  class LayerWriteError(layerId: LayerId, msg: String)
      extends CatalogError(s"Failed to write ${layerId}: $msg")

  class AttributeNotFoundError(attributeName: String, layerId: LayerId)
    extends CatalogError(s"Attribute $attributeName not found for layer $layerId")

  class TileNotFoundError(key: Any, layerId: LayerId)
    extends CatalogError(s"Tile with key $key not found for layer $layerId")

  class MultipleMatchError(layerId: LayerId)
    extends CatalogError(s"Multiple layers match id of $layerId")

  class MultipleAttributesError(attributeName: String, layerId: LayerId)
    extends CatalogError(s"Multiple attributes found for $attributeName for layer $layerId")
    
}
