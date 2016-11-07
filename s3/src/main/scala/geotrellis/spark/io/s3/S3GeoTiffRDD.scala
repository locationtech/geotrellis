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

/**
 * The S3GeoTiffRDD object allows for the creation of whole or windowed RDD[(K, V)]s
 * from files on S3.
 */
object S3GeoTiffRDD {
  /**
   * This case class contains the various parameters one can set when reading
   * RDDs from S3 using Spark.
   *
   * @param crs: What [[CRS]] K should be in. If `None`, the reader will use the file's original CRS.
   * @param timeTag: A `String` that states the temporal information about the tile. Uses the default timeTag
   *   if none is supplied.
   * @param timeForamt: A `String` that states how the time should be formatted. Uses the default style if not set.
   * @param maxTileSize: How big the sub-tiles of the given tile should be. For example, inputting Some(300) will return
   *   multiple RDDs with tiles of dimension 300x300. If no size is given, then the file is not windowed.
   * @param numPartitions: How many partitions Spark should create when it repartitions the data. Default is `None`.
   * @param chunkSize: How many bytes should be read in at a time. Default is `None`.
   * @param getS3Client: A funciton that is parameterless and returns an S3Client.
   *   Defaults to a S3Client with default configurations.
   *
   * @return A new istance of Options that contains the RDD creation settings.
   */
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

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def apply[K, V](bucket: String, prefix: String)(implicit sc: SparkContext, gif: GeoTiffS3InputFormattable[K, V]): RDD[(K, V)] =
    apply(bucket, prefix, Options.DEFAULT)

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
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

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param objectRequestToDimensions: A RDD that contains the GetObjectRequest of a given GeoTiff
   *   and the cols and rows to be read in represented as a (Int, Int).
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
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

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   *
   * @return RDD[(ProjectedExtent, Tile)](s)
   */
  def spatial(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(bucket, prefix, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(ProjectedExtent, Tile)](s)
   */
  def spatial(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    apply[ProjectedExtent, Tile](bucket, prefix, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultiibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   *
   * @return RDD[(ProjectedExtent, MultibandTile)](s)
   */
  def spatialMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(bucket, prefix, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultiibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(ProjectedExtent, MultibandTile)](s)
   */
  def spatialMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    apply[ProjectedExtent, MultibandTile](bucket, prefix, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   *
   * @return RDD[(TemporalProjectedExtent, Tile)](s)
   */
  def temporal(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(bucket, prefix, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(TemporalProjectedExtent, Tile)](s)
   */
  def temporal(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    apply[TemporalProjectedExtent, Tile](bucket, prefix, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultiibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   *
   * @return RDD[(TemporalProjectedExtent, MultibandTile)](s)
   */
  def temporalMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(bucket, prefix, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultiibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(TemporalProjectedExtent, MultibandTile)](s)
   */
  def temporalMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    apply[TemporalProjectedExtent, MultibandTile](bucket, prefix, options)
}
