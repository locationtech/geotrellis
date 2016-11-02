package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.s3.util.S3BytesStreamer
import geotrellis.util.StreamByteReader
import geotrellis.vector._

import org.apache.hadoop.mapreduce.InputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spire.syntax.cfor._

object S3GeoTiffRDD {

  def apply[K, V](bucket: String, prefix: String, maxTileSize: Option[Int])(implicit client: S3Client, sc: SparkContext): RDD[(K, V)] = ???

  def spatial(bucket: String, prefix: String)(implicit client: S3Client, sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(bucket, prefix, None)

  def spatial(bucket: String, prefix: String, maxTileSize: Option[Int])(implicit client: S3Client, sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    apply[ProjectedExtent, Tile](bucket, prefix, maxTileSize)
  
  def spatialMultiband(bucket: String, prefix: String)(implicit client: S3Client, sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(bucket, prefix, None)

  def spatialMultiband(bucket: String, prefix: String, maxTileSize: Option[Int])(implicit client: S3Client, sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    apply[ProjectedExtent, MultibandTile](bucket, prefix, maxTileSize)
  
  def temporal(bucket: String, prefix: String)(implicit client: S3Client, sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(bucket, prefix, None)
  
  def temporal(bucket: String, prefix: String, maxTileSize: Option[Int])(implicit client: S3Client, sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    apply[TemporalProjectedExtent, Tile](bucket, prefix, maxTileSize)
  
  def temporalMultiband(bucket: String, prefix: String)(implicit client: S3Client, sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(bucket, prefix, None)
  
  def temporalMultiband(bucket: String, prefix: String, maxTileSize: Option[Int])(implicit client: S3Client, sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    apply[TemporalProjectedExtent, MultibandTile](bucket, prefix, maxTileSize)
}
