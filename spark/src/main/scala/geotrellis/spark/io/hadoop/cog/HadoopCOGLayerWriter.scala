package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.hadoop.{HadoopAttributeStore, HdfsUtils}
import geotrellis.spark.io.index._

import org.apache.hadoop.fs.Path
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HadoopCOGLayerWriter(
  val attributeStore: HadoopAttributeStore
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]]
  ): Unit = {
    val catalogPath = attributeStore.rootPath

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val keyPath =
        (key: K) =>
          s"hdfs://${catalogPath.toString}/${layerName}/" +
          s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
          s"${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      cogLayer.layers(zoomRange).foreach { case (key, cog) =>
        HdfsUtils.write(
          new Path(s"${keyPath(key)}.${Extension}"),
          attributeStore.hadoopConfiguration
        ) { new GeoTiffWriter(cog, _).write(true) }
      }
    }
  }
}
