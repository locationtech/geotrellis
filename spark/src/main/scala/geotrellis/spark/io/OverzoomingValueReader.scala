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

case class OverzoomingValueReader(valueReader: ValueReader[LayerId], resampleMethod: ResampleMethod) {
  val attributeStore = valueReader.attributeStore

  def reader[K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag, 
             V <: CellGrid: AvroRecordCodec: ? => TileResampleMethods[V]
  ](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val LayerId(layerName, requestedZoom) = layerId
    val maxAvailableZoom = valueReader.attributeStore.layerIds.filter { case LayerId(name, _) => name == layerName }.map(_.zoom).max
    lazy val baseReader = valueReader.reader[K, V](layerId)
    lazy val maxReader = valueReader.reader[K, V](LayerId(layerName, maxAvailableZoom))

    def read(key: K): V = {
      if (requestedZoom <= maxAvailableZoom) {
        return baseReader.read(key)
      } else {
        val maxKey = {
          val srcSK = key.getComponent[SpatialKey]
          val denom = math.pow(2, requestedZoom - maxAvailableZoom).toInt
          key.setComponent[SpatialKey](SpatialKey(srcSK._1 / denom, srcSK._2 / denom))
        }

        val metadata = valueReader.attributeStore.readMetadata[TileLayerMetadata[K]](LayerId(layerName, maxAvailableZoom))
        val layoutScheme = ZoomedLayoutScheme(metadata.crs, metadata.tileRows)

        val requestedMaptrans = layoutScheme.levelForZoom(requestedZoom).layout.mapTransform
        val maxMaptrans = metadata.mapTransform

        val toResample = maxReader.read(maxKey)

        return toResample.resample(maxMaptrans(maxKey), RasterExtent(requestedMaptrans(key), toResample.cols, toResample.rows), resampleMethod)
      }
    }
  }
}

object OverzoomingValueReader {

  def apply(attributeStore: AttributeStore, valueReaderUri: URI, resampleMethod: ResampleMethod): OverzoomingValueReader =
    OverzoomingValueReader(ValueReader(attributeStore, valueReaderUri), resampleMethod)

  def apply(attributeStoreUri: URI, valueReaderUri: URI, resampleMethod: ResampleMethod): OverzoomingValueReader =
    apply(AttributeStore(attributeStoreUri), valueReaderUri, resampleMethod)

  def apply(uri: URI, resampleMethod: ResampleMethod): OverzoomingValueReader =
    apply(attributeStoreUri = uri, valueReaderUri = uri, resampleMethod)

  def apply(attributeStore: AttributeStore, valueReaderUri: String, resampleMethod: ResampleMethod): OverzoomingValueReader =
    apply(attributeStore, new URI(valueReaderUri), resampleMethod)

  def apply(attributeStoreUri: String, valueReaderUri: String, resampleMethod: ResampleMethod): OverzoomingValueReader =
    apply(AttributeStore(new URI(attributeStoreUri)), new URI(valueReaderUri), resampleMethod)

  def apply(uri: String, resampleMethod: ResampleMethod): OverzoomingValueReader = {
    val _uri = new URI(uri)
    apply(attributeStoreUri = _uri, valueReaderUri = _uri, resampleMethod)
  }

  def apply(attributeStore: AttributeStore, valueReaderUri: URI): OverzoomingValueReader =
    OverzoomingValueReader(ValueReader(attributeStore, valueReaderUri), ResampleMethod.DEFAULT)

  def apply(attributeStoreUri: URI, valueReaderUri: URI): OverzoomingValueReader =
    apply(AttributeStore(attributeStoreUri), valueReaderUri, ResampleMethod.DEFAULT)

  def apply(uri: URI): OverzoomingValueReader =
    apply(attributeStoreUri = uri, valueReaderUri = uri, ResampleMethod.DEFAULT)

  def apply(attributeStore: AttributeStore, valueReaderUri: String): OverzoomingValueReader =
    apply(attributeStore, new URI(valueReaderUri), ResampleMethod.DEFAULT)

  def apply(attributeStoreUri: String, valueReaderUri: String): OverzoomingValueReader =
    apply(AttributeStore(new URI(attributeStoreUri)), new URI(valueReaderUri), ResampleMethod.DEFAULT)

  def apply(uri: String): OverzoomingValueReader = {
    val _uri = new URI(uri)
    apply(attributeStoreUri = _uri, valueReaderUri = _uri, ResampleMethod.DEFAULT)
  }

}
