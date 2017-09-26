package geotrellis.spark.io

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.spark.tiling._
import geotrellis.util._
import spray.json._
import scala.reflect._
import java.net.URI

trait OverzoomingValueReader {
  self: ValueReader[LayerId] =>

  def overzoomingReader[K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag, 
             V <: CellGrid: AvroRecordCodec: ? => TileResampleMethods[V]
  ](layerId: LayerId, resampleMethod: ResampleMethod = ResampleMethod.DEFAULT): Reader[K, V] = new Reader[K, V] {
    val LayerId(layerName, requestedZoom) = layerId
    val maxAvailableZoom = attributeStore.layerIds.filter { case LayerId(name, _) => name == layerName }.map(_.zoom).max
    val metadata = attributeStore.readMetadata[TileLayerMetadata[K]](LayerId(layerName, maxAvailableZoom))

    val layoutScheme = ZoomedLayoutScheme(metadata.crs, metadata.tileRows)
    val requestedMaptrans = layoutScheme.levelForZoom(requestedZoom).layout.mapTransform
    val maxMaptrans = metadata.mapTransform

    lazy val baseReader = reader[K, V](layerId)
    lazy val maxReader = reader[K, V](LayerId(layerName, maxAvailableZoom))

    def read(key: K): V = {
      if (requestedZoom <= maxAvailableZoom) {
        return baseReader.read(key)
      } else {
        val maxKey = {
          val srcSK = key.getComponent[SpatialKey]
          val denom = math.pow(2, requestedZoom - maxAvailableZoom).toInt
          key.setComponent[SpatialKey](SpatialKey(srcSK._1 / denom, srcSK._2 / denom))
        }

        val toResample = maxReader.read(maxKey)

        return toResample.resample(maxMaptrans(maxKey), RasterExtent(requestedMaptrans(key), toResample.cols, toResample.rows), resampleMethod)
      }
    }
  }
}

