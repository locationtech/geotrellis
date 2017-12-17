package geotrellis.spark.io.s3.geotiff

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy, SinglebandGeoTiff}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.io.hadoop.geotiff.{AttributeStore, GeoTiffLayerReader, GeoTiffMetadata}
import geotrellis.util.StreamingByteReader
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.io.s3.S3Client

import com.amazonaws.services.s3.AmazonS3URI

import java.net.URI

/** Approach with TiffTags stored in a DB */
case class S3GeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  getS3Client: () => S3Client = () => S3Client.DEFAULT
) extends GeoTiffLayerReader[M] {
  protected def readSingleband(uri: URI): SinglebandGeoTiff = {
    val auri = new AmazonS3URI(uri)
    GeoTiffReader
      .readSingleband(
        StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key = auri.getKey,
            client = getS3Client()
          )
        ),
        false,
        true
      )
  }
}
