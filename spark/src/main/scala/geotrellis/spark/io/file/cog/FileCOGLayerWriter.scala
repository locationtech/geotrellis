package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT
import geotrellis.spark.io.cog.vrt.VRT.IndexedSimpleSource
import geotrellis.spark.io.file._
import geotrellis.spark.io.index._
import geotrellis.util.Filesystem

import spray.json.JsonFormat
import java.io.File

import scala.reflect.ClassTag

class FileCOGLayerWriter(
  val attributeStore: FileAttributeStore
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid: TiffMethods: ClassTag](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]],
    mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]] = None
  ): Unit = {
    /** Collect VRT into accumulators, to write everything and to collect VRT at the same time */
    val sc = cogLayer.layers.head._2.sparkContext
    val samplesAccumulator = sc.collectionAccumulator[IndexedSimpleSource](s"vrt_samples_$layerName")

    val catalogPath = new File(attributeStore.catalogPath).getAbsolutePath
    Filesystem.ensureDirectory(new File(catalogPath, layerName).getAbsolutePath)

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val keyPath = KeyPathGenerator(catalogPath, s"${layerName}/${zoomRange.slug}", keyIndex, maxWidth)
      Filesystem.ensureDirectory(new File(catalogPath, s"${layerName}/${zoomRange.slug}").getAbsolutePath)

      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))

      // Write each cog layer for each zoom range, starting from highest zoom levels.
      cogLayer.layers(zoomRange).foreach { case (key, cog) =>
        cog.write(s"${keyPath(key)}.${Extension}", true)

        // collect VRT metadata
        (0 until geoTiffBandsCount(cog))
          .map { b =>
            val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
            (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent))
          }
          .foreach(samplesAccumulator.add)
      }

      vrt
        .fromAccumulator(samplesAccumulator)
        .write(s"${catalogPath}/${layerName}/${zoomRange.slug}/vrt.xml")

      samplesAccumulator.reset
    }
  }
}

object FileCOGLayerWriter {
  def apply(attributeStore: FileAttributeStore): FileCOGLayerWriter =
    new FileCOGLayerWriter(attributeStore)

  def apply(catalogPath: String): FileCOGLayerWriter =
    apply(FileAttributeStore(catalogPath))
}
