package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.hadoop.geotiff._

import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.ObjectMetadata
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.ByteArrayInputStream
import java.net.URI

import scala.io.Source

object S3JsonGeoTiffAttributeStore {
  def readData(uri: URI, getS3Client: () => S3Client = () => S3Client.DEFAULT): Seq[GeoTiffMetadata] = {
    val s3Client = getS3Client()
    val s3Uri = new AmazonS3URI(uri)

    val stream =
      s3Client
        .getObject(s3Uri.getBucket, s3Uri.getKey)
        .getObjectContent

    val json = try {
      Source
        .fromInputStream(stream)
        .getLines
        .mkString(" ")
    } finally stream.close()

    json
      .parseJson
      .convertTo[Seq[GeoTiffMetadata]]
  }


  def apply(uri: URI): JsonGeoTiffAttributeStore =
    JsonGeoTiffAttributeStore(uri, readData(_, () => S3Client.DEFAULT))

  def apply(
    path: URI,
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ): JsonGeoTiffAttributeStore = {
    val s3Client = getS3Client()
    val s3Path = new AmazonS3URI(path)
    val data = S3GeoTiffInput.list(name, uri, pattern, recursive)
    val attributeStore = JsonGeoTiffAttributeStore(path, readData(_, () => S3Client.DEFAULT))

    val str = data.toJson.compactPrint
    val is = new ByteArrayInputStream(str.getBytes("UTF-8"))
    s3Client.putObject(s3Path.getBucket, s3Path.getKey, is, new ObjectMetadata())

    attributeStore
  }
}
