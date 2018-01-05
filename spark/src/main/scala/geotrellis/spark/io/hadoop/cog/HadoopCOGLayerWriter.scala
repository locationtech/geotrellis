package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT
import geotrellis.spark.io.hadoop.{HadoopAttributeStore, HdfsUtils}
import geotrellis.spark.io.index._

import org.apache.hadoop.fs.Path
import spray.json.JsonFormat

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.xml.Elem

import scala.reflect.ClassTag

class HadoopCOGLayerWriter(
  val attributeStore: HadoopAttributeStore
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid: ClassTag](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]]
  ): Unit = {
    /** Collect VRT into accumulators, to write everything and to collect VRT at the same time */
    val sc = cogLayer.layers.head._2.sparkContext
    val samplesAccumulator = sc.collectionAccumulator[(String, (Int, Elem))](s"vrt_samples_$layerName")

    val catalogPath = attributeStore.rootPath

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))
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

        // collect VRT metadata
        val bands = geoTiffBandsCount(cog)
        (0 until bands)
          .map { b =>
            val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
            (s"$idx", vrt.simpleSource(s"$idx.$Extension", b + 1)(cog.cols, cog.rows)(cog.extent))
          }
          .foreach(samplesAccumulator.add)
      }

      val os =
        vrt
          .fromSimpleSources(
            samplesAccumulator
              .value
              .asScala
              .toList
              .sortBy(_._1.toLong)
              .map(_._2)
          )
          .outputStream

      HdfsUtils.write(
        new Path(s"hdfs://${catalogPath.toString}/${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}/vrt.xml"),
        sc.hadoopConfiguration
      ) { _.write(os.toByteArray) }

      samplesAccumulator.reset
    }
  }
}
