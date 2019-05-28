package geotrellis

import geotrellis.tiling.{KeyBounds, MapKeyTransform}
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

import scala.util.{Failure, Success, Try}

package object layers extends Implicits {
  type RasterCollection[M] = Seq[Raster[Tile]] with Metadata[M]
  type MultibandRasterCollection[M] = Seq[Raster[MultibandTile]] with Metadata[M]

  type TileLayerCollection[K] = Seq[(K, Tile)] with Metadata[TileLayerMetadata[K]]
  object TileLayerCollection {
    def apply[K](seq: Seq[(K, Tile)], metadata: TileLayerMetadata[K]): TileLayerCollection[K] =
      new ContextCollection(seq, metadata)
  }

  type MultibandTileLayerCollection[K] = Seq[(K, MultibandTile)] with Metadata[TileLayerMetadata[K]]
  object MultibandTileLayerCollection {
    def apply[K](seq: Seq[(K, MultibandTile)], metadata: TileLayerMetadata[K]): MultibandTileLayerCollection[K] =
      new ContextCollection(seq, metadata)
  }

  implicit class TryOption[T](option: Option[T]) {
    def toTry(exception: => Throwable): Try[T] =
      option match {
        case Some(t) => Success(t)
        case None    => Failure(exception)
      }
  }

  implicit class GeoTiffInfoMethods(that: GeoTiffReader.GeoTiffInfo) {
    def mapTransform =
      MapKeyTransform(
        extent = that.extent,
        layoutCols = that.segmentLayout.tileLayout.layoutCols,
        layoutRows = that.segmentLayout.tileLayout.layoutRows)
  }

  // Custom exceptions
  class LayerIOError(val message: String) extends Exception(message)

  class AvroLayerAttributeError(attributeName: String, layerId: LayerId)
    extends LayerIOError(s"AvroLayer: $layerId does not have the attribute: $attributeName")

  class COGLayerAttributeError(attributeName: String, layerId: LayerId)
    extends LayerIOError(s"COGLayer: $layerId does not have the attribute: $attributeName")

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

  class ValueNotFoundError(key: Any, layerId: LayerId)
    extends LayerIOError(s"Value with key $key not found for layer $layerId")

  class HeaderMatchError[T <: Product](layerId: LayerId, headerl: T, headerr: T)
    extends LayerIOError(s"Layer $layerId Header data ($headerl) not matches ($headerr)")

  class LayerOutOfKeyBoundsError(layerId: LayerId, bounds: KeyBounds[_])
    extends LayerIOError(s"Updating rdd is out of the key index space for $layerId: $bounds. You must reindex this layer with large enough key bounds for this update.")

  class LayerEmptyBoundsError(layerId: LayerId)
      extends LayerIOError(s"Layer $layerId contains empty bounds; is this layer corrupt?")
}
