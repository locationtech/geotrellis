package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io.index.{Index, KeyIndex}
import geotrellis.spark.io.s3.{S3AttributeStore, S3Client, S3RDDWriter, makePath}
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT
import geotrellis.spark.io.cog.vrt.VRT.IndexedSimpleSource

import spray.json.JsonFormat
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}

import java.io.ByteArrayInputStream

import scala.reflect.ClassTag

class S3COGLayerWriter(
  val attributeStore: S3AttributeStore,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  threads: Int = S3RDDWriter.DefaultThreadCount
) extends COGLayerWriter {
  def writeCOGLayer[K: SpatialComponent: Ordering: JsonFormat: ClassTag, V <: CellGrid: ClassTag](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]]
  ): Unit = {
    /** Collect VRT into accumulators, to write everything and to collect VRT at the same time */
    val sc = cogLayer.layers.head._2.sparkContext
    val samplesAccumulator = sc.collectionAccumulator[IndexedSimpleSource](s"vrt_samples_$layerName")

    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)
    attributeStore.write(LayerId(layerName, 0), "cog_metadata", storageMetadata)

    val (bucket, keyPrefix) = attributeStore.bucket -> attributeStore.prefix

    val s3Client = getS3Client()
    for(zoomRange <- cogLayer.layers.keys.toSeq.sorted(Ordering[ZoomRange].reverse)) {
      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val prefix = makePath(keyPrefix, s"${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}")
      val keyPath = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))

      // Write each cog layer for each zoom range, starting from highest zoom levels.
      cogLayer.layers(zoomRange).foreachPartition { partition =>
        val s3Client = getS3Client()
        partition.foreach { case (key, cog) =>
          val bytes = GeoTiffWriter.write(cog, true)
          val objectMetadata = new ObjectMetadata()
          objectMetadata.setContentLength(bytes.length)
          val is = new ByteArrayInputStream(bytes)
          val request = new PutObjectRequest(bucket, s"${keyPath(key)}.${Extension}", is, objectMetadata)
          s3Client.putObject(request)

          // collect VRT metadata
          (0 until geoTiffBandsCount(cog))
            .map { b =>
              val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
              (idx.toLong, vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent))
            }
            .foreach(samplesAccumulator.add)
        }
      }

      val bytes =
        vrt
          .fromAccumulator(samplesAccumulator)
          .outputStream
          .toByteArray

      val objectMetadata = new ObjectMetadata()
      objectMetadata.setContentLength(bytes.length)
      val is = new ByteArrayInputStream(bytes)

      val request = new PutObjectRequest(
        bucket,
        s"${keyPrefix}/${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}/vrt.xml",
        is,
        objectMetadata
      )
      s3Client.putObject(request)

      samplesAccumulator.reset
    }
  }
}
