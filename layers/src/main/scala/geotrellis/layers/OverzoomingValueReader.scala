package geotrellis.layers

import geotrellis.tiling.{SpatialComponent, SpatialKey, ZoomedLayoutScheme}
import geotrellis.raster.resample.{ResampleMethod, TileResampleMethods}
import geotrellis.raster.{CellGrid, RasterExtent}
import geotrellis.layers.avro.AvroRecordCodec
import geotrellis.util._

import spray.json.JsonFormat

import scala.reflect.ClassTag

trait OverzoomingValueReader extends ValueReader[LayerId] {
  def overzoomingReader[
    K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: AvroRecordCodec: ? => TileResampleMethods[V]
  ](layerId: LayerId, resampleMethod: ResampleMethod): Reader[K, V] = new Reader[K, V] {
    val LayerId(layerName, requestedZoom) = layerId
    val maxAvailableZoom = attributeStore.availableZoomLevels(layerName).max
    val metadata = attributeStore.readMetadata[TileLayerMetadata[K]](LayerId(layerName, maxAvailableZoom))

    val layoutScheme = ZoomedLayoutScheme(metadata.crs, metadata.tileRows)
    val requestedMaptrans = layoutScheme.levelForZoom(requestedZoom).layout.mapTransform
    val maxMaptrans = metadata.mapTransform

    lazy val baseReader = reader[K, V](layerId)
    lazy val maxReader = reader[K, V](LayerId(layerName, maxAvailableZoom))

    def read(key: K): V =
      if (requestedZoom <= maxAvailableZoom) {
        baseReader.read(key)
      } else {
        val maxKey = {
          val srcSK = key.getComponent[SpatialKey]
          val denom = math.pow(2, requestedZoom - maxAvailableZoom).toInt
          key.setComponent[SpatialKey](SpatialKey(srcSK._1 / denom, srcSK._2 / denom))
        }

        val toResample = maxReader.read(maxKey)

        toResample.resample(
          maxMaptrans.keyToExtent(maxKey.getComponent[SpatialKey]),
          RasterExtent(requestedMaptrans.keyToExtent(key.getComponent[SpatialKey]), toResample.cols, toResample.rows),
          resampleMethod
        )
      }
  }
}
