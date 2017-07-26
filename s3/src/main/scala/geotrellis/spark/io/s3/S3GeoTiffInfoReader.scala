package geotrellis.spark.io.s3

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.io._
import geotrellis.spark.io.s3.util.S3RangeReader

import com.amazonaws.services.s3.model.ListObjectsRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class S3GeoTiffInfoReader(
  bucket: String,
  prefix: String,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  delimiter: Option[String] = None,
  decompress: Boolean = false,
  streaming: Boolean = true
) extends GeoTiffInfoReader {
  lazy val geoTiffInfo: List[(String, GeoTiffReader.GeoTiffInfo)] = {
    val s3Client = getS3Client()

    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    s3Client
      .listKeys(listObjectsRequest)
      .toList
      .map(key => (key, GeoTiffReader.readGeoTiffInfo(S3RangeReader(bucket, key, s3Client), decompress, streaming)))
  }

  def geoTiffInfoRdd(implicit sc: SparkContext): RDD[(String, GeoTiffReader.GeoTiffInfo)] = {
    val listObjectsRequest =
      delimiter
        .fold(new ListObjectsRequest(bucket, prefix, null, null, null))(new ListObjectsRequest(bucket, prefix, null, _, null))

    sc.parallelize(getS3Client().listKeys(listObjectsRequest))
      .map(key => (key, GeoTiffReader.readGeoTiffInfo(S3RangeReader(bucket, key, getS3Client()), decompress, streaming)))
  }
}

object S3GeoTiffInfoReader {
  def apply(
    bucket: String,
    prefix: String,
    options: S3GeoTiffRDD.Options
  ): S3GeoTiffInfoReader = S3GeoTiffInfoReader(bucket, prefix, options.getS3Client, options.delimiter)
}
