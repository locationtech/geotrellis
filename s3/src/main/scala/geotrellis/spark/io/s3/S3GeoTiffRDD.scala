package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.vector._

import org.apache.hadoop.mapreduce.InputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.amazonaws.services.s3.model._

import spire.syntax.cfor._

object S3GeoTiffRDD {
  case class Options(
    crs: Option[CRS] = None,
    timeTag: String = TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    maxTileSize: Option[Int] = None,
    numPartitions: Option[Int] = None,
    chunkSize: Option[Int] = None,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply[K, V](bucket: String, prefix: String)(implicit sc: SparkContext, gif: GeoTiffS3InputFormattable[K, V]): RDD[(K, V)] =
    apply(bucket, prefix, Options.DEFAULT)

  def apply[K, V](bucket: String, prefix: String, options: Options)(implicit sc: SparkContext, gif: GeoTiffS3InputFormattable[K, V]): RDD[(K, V)] =
    options.maxTileSize match {
      case Some(tileSize) =>

        val conf = sc.hadoopConfiguration
        S3InputFormat.setCreateS3Client(conf, options.getS3Client)

        val objectRequestsToDimensions: RDD[(GetObjectRequest, (Int, Int))] =
          sc.newAPIHadoopRDD(
            conf,
            classOf[TiffTagsS3InputFormat],
            classOf[GetObjectRequest],
            classOf[TiffTags]
          ).mapValues { tiffTags => (tiffTags.cols, tiffTags.rows) }

        apply[K, V](objectRequestsToDimensions, options)
      case None =>
        gif.load(bucket, prefix, options)
    }

  def apply[K, V](objectRequestsToDimensions: RDD[(GetObjectRequest, (Int, Int))], options: Options)
                 (implicit sc: SparkContext, gif: GeoTiffS3InputFormattable[K, V]): RDD[(K, V)] = {
    val windows =
      objectRequestsToDimensions
        .flatMap { case (objectRequest, (cols, rows)) =>
          val result = scala.collection.mutable.ListBuffer[GridBounds]()

          options.maxTileSize match {
            case Some(tileSize) =>
              cfor(0)(_ < cols, _ + tileSize) { col =>
                cfor(0)(_ < rows, _ + tileSize) { row =>
                  result +=
                  GridBounds(
                    col,
                    row,
                    math.min(col + tileSize - 1, cols - 1),
                    math.min(row + tileSize - 1, rows - 1)
                  )
                }
              }
            case None =>
              result += GridBounds(0, 0, cols - 1, rows - 1)
          }
          result.map((objectRequest, _))
        }

    val repartitioned =
      options.numPartitions match {
        case Some(p) => windows.repartition(p)
        case None => windows
      }

    gif.load(repartitioned, options)
  }

  def spatial(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(bucket, prefix, Options.DEFAULT)

  def spatial(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    apply[ProjectedExtent, Tile](bucket, prefix, options)

  def spatialMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(bucket, prefix, Options.DEFAULT)

  def spatialMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    apply[ProjectedExtent, MultibandTile](bucket, prefix, options)

  def temporal(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(bucket, prefix, Options.DEFAULT)

  def temporal(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    apply[TemporalProjectedExtent, Tile](bucket, prefix, options)

  def temporalMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(bucket, prefix, Options.DEFAULT)

  def temporalMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    apply[TemporalProjectedExtent, MultibandTile](bucket, prefix, options)
}
