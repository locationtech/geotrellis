package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.spark.io.s3.S3Client

import java.net.URI

object S3IMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI,
    pattern: String = "(.)*",
    recursive: Boolean = true,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ): InMemoryGeoTiffAttributeStore =
    InMemoryGeoTiffAttributeStore(() => S3GeoTiffInput.list(name, uri, pattern, recursive, getS3Client))
}
