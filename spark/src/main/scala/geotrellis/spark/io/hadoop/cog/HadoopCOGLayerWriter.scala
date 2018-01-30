package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io.InvalidLayerIdError
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT
import geotrellis.spark.io.cog.vrt.VRT.IndexedSimpleSource
import geotrellis.spark.io.hadoop.{HadoopAttributeStore, HdfsUtils}
import geotrellis.spark.io.index._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HadoopCOGLayerWriter(
  val attributeStore: HadoopAttributeStore
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

    def catalogPath = attributeStore.rootPath

    try {
      attributeStore.attributePath(LayerId(layerName, 0), "cog_metadata")
    } catch {
      case e: Exception =>
        throw new InvalidLayerIdError(LayerId(layerName, 0)).initCause(e)
    }

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val keyPath =
        (key: K) =>
          s"${catalogPath.toString}/${layerName}/" +
          s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
          s"${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      cogLayer.layers(zoomRange).foreach { case (key, cog) =>
        HdfsUtils.write(
          new Path(s"${keyPath(key)}.${Extension}"),
          attributeStore.hadoopConfiguration
        ) { new GeoTiffWriter(cog, _).write(true) }

        // collect VRT metadata
        (0 until geoTiffBandsCount(cog))
          .map { b =>
            val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
            (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent))
          }
          .foreach(samplesAccumulator.add)
      }

      val os =
        vrt
          .fromAccumulator(samplesAccumulator)
          .outputStream

      HdfsUtils.write(
        new Path(s"${catalogPath.toString}/${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}/vrt.xml"),
        attributeStore.hadoopConfiguration
      ) { _.write(os.toByteArray) }

      samplesAccumulator.reset
    }
  }
}

object HadoopCOGLayerWriter {
  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGLayerWriter =
    new HadoopCOGLayerWriter(HadoopAttributeStore(rootPath))
}

