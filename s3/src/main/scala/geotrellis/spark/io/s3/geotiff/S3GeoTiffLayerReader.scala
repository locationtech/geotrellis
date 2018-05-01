package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.s3.cog.byteReader
import geotrellis.spark.io.s3.conf.S3Config
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.io.hadoop.geotiff.{AttributeStore, GeoTiffLayerReader, GeoTiffMetadata}
import geotrellis.util.ByteReader
import geotrellis.spark.io.s3.S3Client

import java.net.URI

case class S3GeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  defaultThreads: Int = S3GeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, getS3Client())
}

object S3GeoTiffLayerReader {
  val defaultThreadCount: Int = S3Config.threads.collection.readThreads
}
