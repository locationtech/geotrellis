package geotrellis.spark

import spray.json.JsonFormat
import scala.util.{Failure, Success, Try}

package object io extends json.Implicits with avro.codecs.Implicits {
  implicit class TryOption[T](option: Option[T]) {
    def toTry(exception: => Throwable): Try[T] =
      option match {
        case Some(t) => Success(t)
        case None    => Failure(exception)
      }
  }

  // Custom exceptions
  class LayerIOError(val message: String) extends Exception(message)

  class LayerReadError(layerId: LayerId)
    extends LayerIOError(s"LayerMetadata not found for layer $layerId")

  class LayerExistsError(layerId: LayerId)
    extends LayerIOError(s"Layer $layerId already exists in the catalog")

  class LayerNotFoundError(layerId: LayerId)
    extends LayerIOError(s"Layer $layerId not found in the catalog")

  class InvalidLayerIdError(layerId: LayerId)
    extends LayerIOError(s"Invalid layer name: $layerId")

  class LayerWriteError(layerId: LayerId, message: String = "")
    extends LayerIOError(s"Failed to write $layerId" + (if (message.nonEmpty) ": " + message else message))

  class LayerUpdateError(layerId: LayerId, message: String = "")
    extends LayerIOError(s"Failed to update $layerId $message" + (if (message.nonEmpty) ": " + message else message))

  class LayerDeleteError(layerId: LayerId)
    extends LayerIOError(s"Failed to delete $layerId")

  class LayerReindexError(layerId: LayerId)
    extends LayerIOError(s"Failed to reindex $layerId")

  class LayerCopyError(from: LayerId, to: LayerId)
    extends LayerIOError(s"Failed to copy $from to $to")

  class LayerMoveError(from: LayerId, to: LayerId)
    extends LayerIOError(s"Failed to move $from to $to")

  class AttributeNotFoundError(attributeName: String, layerId: LayerId)
    extends LayerIOError(s"Attribute $attributeName not found for layer $layerId")

  class TileNotFoundError(key: Any, layerId: LayerId)
    extends LayerIOError(s"Tile with key $key not found for layer $layerId")

  class HeaderMatchError[T <: Product](layerId: LayerId, headerl: T, headerr: T)
    extends LayerIOError(s"Layer $layerId Header data ($headerl) not matches ($headerr)")

  class LayerOutOfKeyBoundsError(layerId: LayerId, bounds: KeyBounds[_])
    extends LayerIOError(s"Updating rdd is out of the key index space for $layerId: $bounds. You must reindex this layer with large enough key bounds for this update.")

  class LayerEmptyBoundsError(layerId: LayerId)
      extends LayerIOError(s"Layer $layerId contains empty bounds; is this layer corrupt?")
}
