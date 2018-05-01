package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.spark.io.s3.S3Client

import spray.json._
import spray.json.DefaultJsonProtocol._
import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.ObjectMetadata

import java.io.ByteArrayInputStream
import java.net.URI

object S3IMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean,
    getS3Client: () => S3Client
  ): InMemoryGeoTiffAttributeStore =
    apply(() => S3GeoTiffInput.list(name, uri, pattern, recursive, getS3Client), getS3Client)

  def apply(
    name: String,
    uri: URI,
    pattern: String
  ): InMemoryGeoTiffAttributeStore = apply(name, uri, pattern, true, () => S3Client.DEFAULT)

  def apply(getDataFunction: () => List[GeoTiffMetadata]): InMemoryGeoTiffAttributeStore =
    apply(getDataFunction, () => S3Client.DEFAULT)

  def apply(
    getDataFunction: () => List[GeoTiffMetadata],
    getS3Client: () => S3Client
  ): InMemoryGeoTiffAttributeStore =
    new InMemoryGeoTiffAttributeStore(() => GeoTiffMetadataTree.fromGeoTiffMetadataList(getDataFunction())) {
      def persist(uri: URI): Unit = {
        val s3Client = getS3Client()
        val s3Path = new AmazonS3URI(uri)
        val data = getDataFunction()

        val str = data.toJson.compactPrint
        val is = new ByteArrayInputStream(str.getBytes("UTF-8"))
        s3Client.putObject(s3Path.getBucket, s3Path.getKey, is, new ObjectMetadata())
      }
    }
}
