package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.StreamingByteReader
import geotrellis.vector.ProjectedExtent

import org.apache.hadoop.conf.Configuration
import com.amazonaws.auth.AWSCredentials
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.{InputFormat, Job, JobContext}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.amazonaws.services.s3.model._

import java.lang.Class
import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

/**
 * A type class that is used for determining how to read a spatial GeoTiff off S3 and
 * turn it into a RDD[(ProjectedExtent, V)].
 */
trait GeoTiffS3InputFormattable[K, V] {
  type I <: InputFormat[K, V]

  def formatClass: Class[I]
  def keyClass: Class[K]
  def valueClass: Class[V]

  /**
   * Creates a Configuraiton for a Spark Job based on the options based in.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A [[Configuration]] that is used to create RDDs in a given way.
   */
  def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf =sc.hadoopConfiguration

    S3InputFormat.setBucket(conf, bucket)
    S3InputFormat.setPrefix(conf, prefix)

    options.crs.foreach { crs => GeoTiffS3InputFormat.setCrs(conf, crs) }

    options.numPartitions match {
      case Some(x) => S3InputFormat.setPartitionCount(conf, x)
      case None =>
    }

    conf
  }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param bucket: A String that is name of the bucket on S3 where the files are kept.
   * @param prefix: A String that is a prefix of all of the files on S3 that are to be read in.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def load(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): RDD[(K, V)] = {
    val conf = configuration(bucket, prefix, options)
    S3InputFormat.setCreateS3Client(conf, options.getS3Client)

    sc.newAPIHadoopRDD(
      conf,
      formatClass,
      keyClass,
      valueClass
    )
  }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param windows: An RDD that contains a [[GetObjectRequest]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options): RDD[(K, V)]
}

/**
 * This object extends [[GeotiffS3InputFormattable]] and is used to create RDDs of [[ProjectedExtent]]s and
 * [[Tile]]s.
 */
object SpatialSinglebandGeoTiffS3InputFormattable extends GeoTiffS3InputFormattable[ProjectedExtent, Tile] {
  type I = GeoTiffS3InputFormat

  def formatClass = classOf[GeoTiffS3InputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[Tile]

  /**
   * Creates a RDD[(ProjectedExtent, Tile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[GetObjectRequest]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[ProjectedExtent]] and [[Tile]], respectively.
   */
  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options): RDD[(ProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

/**
 * This object extends [[GeotiffS3InputFormattable]] and is used to create RDDs of [[ProjectedExtent]]s and
 * [[MultibandTile]]s.
 */
object SpatialMultibandGeoTiffS3InputFormattable extends GeoTiffS3InputFormattable[ProjectedExtent, MultibandTile] {
  type I = MultibandGeoTiffS3InputFormat

  def formatClass = classOf[MultibandGeoTiffS3InputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[MultibandTile]

  /**
   * Creates a RDD[(ProjectedExtent, MultibandTile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[GetObjectRequest]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[ProjectedExtent]] and [[MultibandTile]], respectively.
   */
  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options): RDD[(ProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

/**
 * A type class that extends [[GeoTiffS3InputFormattable]] and is used for
 * determining how to read a temporal-spatial GeoTiff off S3 and turn it into a
 * RDD[(TemporalProjectedExtent, V)].
 */
trait TemporalGeoTiffS3InputFormattable[T] extends GeoTiffS3InputFormattable[TemporalProjectedExtent, T]  {
  override def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = super.configuration(bucket, prefix, options)

    TemporalGeoTiffS3InputFormat.setTimeTag(conf, options.timeTag)
    TemporalGeoTiffS3InputFormat.setTimeFormat(conf, options.timeFormat)
    conf
  }

  /**
   * Gets the time that a given GeoTiff has attributed to it.
   *
   * @param geoTiff: [[GeoTiffData]] of a given GeoTiff.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return The [[ZondedDatTime]] property of the GeoTiff.
   */
  def getTime(geoTiff: GeoTiffData, options: S3GeoTiffRDD.Options): ZonedDateTime = {
    val timeTag = options.timeTag
    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val formatter = DateTimeFormatter.ofPattern(options.timeFormat).withZone(ZoneOffset.UTC)
    ZonedDateTime.from(formatter.parse(dateTimeString))
  }
}

/**
 * This object extends [[TemporalGeotiffS3InputFormattable]] and is used to create RDDs of [[TemporalProjectedExtent]]s and
 * [[Tile]]s.
 */
object TemporalSinglebandGeoTiffS3InputFormattable extends TemporalGeoTiffS3InputFormattable[Tile] {
  type I = TemporalGeoTiffS3InputFormat

  def formatClass = classOf[TemporalGeoTiffS3InputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[Tile]

  /**
   * Creates a RDD[(TemporalProjectedExtent, Tile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[GetObjectRequest]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[TemporalProjectedExtent]] and [[Tile]], respectively.
   */
  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options): RDD[(TemporalProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}

/**
 * This object extends [[TemporalGeotiffS3InputFormattable]] and is used to create RDDs of [[TemporalProjectedExtent]]s and
 * [[MultibandTile]]s.
 */
object TemporalMultibandGeoTiffS3InputFormattable extends TemporalGeoTiffS3InputFormattable[MultibandTile] {
  type I = TemporalMultibandGeoTiffS3InputFormat

  def formatClass = classOf[TemporalMultibandGeoTiffS3InputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[MultibandTile]

  /**
   * Creates a RDD[(TemporalProjectedExtent, MultibandTile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[GetObjectRequest]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[TemporalProjectedExtent]] and [[MultibandTile]], respectively.
   */
  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}
