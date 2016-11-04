package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.util.S3BytesStreamer
import geotrellis.util.StreamByteReader
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

trait GeoTiffS3InputFormattable[K, V] {
  type I <: InputFormat[K, V]

  def formatClass: Class[I]
  def keyClass: Class[K]
  def valueClass: Class[V]

  def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val job = Job.getInstance(sc.hadoopConfiguration)
    S3InputFormat.setBucket(job, bucket)
    S3InputFormat.setPrefix(job, prefix)

    options.crs.foreach { crs => S3InputFormat.setCRS(job, crs) }

    options.numPartitions match {
      case Some(x) => S3InputFormat.setPartitionCount(job, x)
      case None =>
    }

    job.getConfiguration
  }

  def load(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): RDD[(K, V)] = {
    val conf = configuration(bucket, prefix, options)
    sc.newAPIHadoopRDD(
      conf,
      formatClass,
      keyClass,
      valueClass
    )
  }

  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options)(implicit client: S3Client): RDD[(K, V)]
}

object SpatialSinglebandGeoTiffS3InputFormattable extends GeoTiffS3InputFormattable[ProjectedExtent, Tile] {
  type I = GeoTiffS3InputFormat

  def formatClass = classOf[GeoTiffS3InputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[Tile]

  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options)(implicit client: S3Client): RDD[(ProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamByteReader(S3BytesStreamer(objectRequest, client))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

object SpatialMultibandGeoTiffS3InputFormattable extends GeoTiffS3InputFormattable[ProjectedExtent, MultibandTile] {
  type I = MultibandGeoTiffS3InputFormat

  def formatClass = classOf[MultibandGeoTiffS3InputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[MultibandTile]

  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options)(implicit client: S3Client): RDD[(ProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamByteReader(S3BytesStreamer(objectRequest, client))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

trait TemporalGeoTiffS3InputFormattable[T] extends GeoTiffS3InputFormattable[TemporalProjectedExtent, T]  {
  override def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = super.configuration(bucket, prefix, options)

    TemporalGeoTiffS3InputFormat.setTimeTag(conf, options.timeTag)
    TemporalGeoTiffS3InputFormat.setTimeFormat(conf, options.timeFormat)
    conf
  }

  def getTime(geoTiff: GeoTiffData, options: S3GeoTiffRDD.Options): ZonedDateTime = {
    val timeTag = options.timeTag
    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val formatter = DateTimeFormatter.ofPattern(options.timeFormat).withZone(ZoneOffset.UTC)
    ZonedDateTime.from(formatter.parse(dateTimeString))
  }
}

object TemporalSinglebandGeoTiffS3InputFormattable extends TemporalGeoTiffS3InputFormattable[Tile] {
  type I = TemporalGeoTiffS3InputFormat

  def formatClass = classOf[TemporalGeoTiffS3InputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[Tile]

  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options)(implicit client: S3Client): RDD[(TemporalProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamByteReader(S3BytesStreamer(objectRequest, client))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}

object TemporalMultibandGeoTiffS3InputFormattable extends TemporalGeoTiffS3InputFormattable[MultibandTile] {
  type I = TemporalMultibandGeoTiffS3InputFormat

  def formatClass = classOf[TemporalMultibandGeoTiffS3InputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[MultibandTile]

  def load(windows: RDD[(GetObjectRequest, GridBounds)], options: S3GeoTiffRDD.Options)(implicit client: S3Client): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (objectRequest, window) =>
        val reader = StreamByteReader(S3BytesStreamer(objectRequest, client))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}
