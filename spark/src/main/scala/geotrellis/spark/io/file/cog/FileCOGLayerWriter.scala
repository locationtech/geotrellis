package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.file._
import geotrellis.spark.io.index._
import geotrellis.util.Filesystem

import spray.json.JsonFormat

import scala.reflect.ClassTag
import java.io.File

class FileCOGLayerWriter(
  val attributeStore: FileAttributeStore
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]]
  ): Unit = {
    val catalogPath = new File(attributeStore.catalogPath).getAbsolutePath
    Filesystem.ensureDirectory(new File(catalogPath, layerName).getAbsolutePath)

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val keyPath = KeyPathGenerator(catalogPath, s"${layerName}/${zoomRange.slug}", keyIndex, maxWidth)
      Filesystem.ensureDirectory(new File(catalogPath, s"${layerName}/${zoomRange.slug}").getAbsolutePath)

      // Write each cog layer for each zoom range, starting from highest zoom levels.
      cogLayer.layers(zoomRange).foreach { case (key, cog) =>
        cog.write(s"${keyPath(key)}.tiff", true)
      }
    }
  }
}
