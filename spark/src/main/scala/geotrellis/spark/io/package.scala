package geotrellis.spark

import geotrellis.spark.io.index.KeyIndexMethod
import spray.json.JsonFormat
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

  class LayerReadError(layerId: LayerId)
    extends CatalogError(s"LayerMetaData not found for layer $layerId")

  class LayerExistsError(layerId: LayerId)
    extends CatalogError(s"Layer $layerId already exists in the catalog")

  class LayerNotExistsError(layerId: LayerId)
    extends CatalogError(s"Layer $layerId not exists in the catalog")

  class LayerWriteError(layerId: LayerId)
    extends CatalogError(s"Failed to write $layerId")

  class LayerUpdateError(layerId: LayerId)
    extends CatalogError(s"Failed to update $layerId")

  class AttributeNotFoundError(attributeName: String, layerId: LayerId)
    extends CatalogError(s"Attribute $attributeName not found for layer $layerId")

  class TileNotFoundError(key: Any, layerId: LayerId)
    extends CatalogError(s"Tile with key $key not found for layer $layerId")

  class HeaderMatchError[T <: Product](layerId: LayerId, headerl: T, headerr: T)
    extends CatalogError(s"Layer $layerId Header data ($headerl) not matches ($headerr)")

  class OutOfKeyBoundsError(layerId: LayerId)
    extends CatalogError(s"Updating rdd is out of $layerId bounds")

  implicit class withJsonAttributeStoreMethods(store: AttributeStore[JsonFormat])
    extends JsonAttributeStoreMethods(store)

  def notIncluded(set: (Long, Long), subset: (Long, Long)): Boolean =
    if(set._1 < subset._1 && set._2 > subset._2) false
    else true
}