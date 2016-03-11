package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector.Extent
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import org.joda.time._
import org.joda.time.format._

object TemporalGeoTiffS3InputFormat {
  final val GEOTIFF_TIME_TAG = "GEOTIFF_TIME_TAG"
  final val GEOTIFF_TIME_FORMAT = "GEOTIFF_TIME_FORMAT"

  def setTimeTag(job: JobContext, timeTag: String): Unit =
    setTimeTag(job.getConfiguration, timeTag)

  def setTimeTag(conf: Configuration, timeTag: String): Unit =
    conf.set(GEOTIFF_TIME_TAG, timeTag)

  def setTimeFormat(job: JobContext, timeFormat: String): Unit =
    setTimeFormat(job.getConfiguration, timeFormat)

  def setTimeFormat(conf: Configuration, timeFormat: String): Unit =
    conf.set(GEOTIFF_TIME_FORMAT, timeFormat)

  def getTimeTag(job: JobContext) =
    job.getConfiguration.get(GEOTIFF_TIME_TAG, "TIFFTAG_DATETIME")

  def getTimeFormatter(job: JobContext): DateTimeFormatter = {
    val df = job.getConfiguration.get(GEOTIFF_TIME_FORMAT)
    if (df == null) { DateTimeFormat.forPattern("YYYY:MM:dd HH:mm:ss") }
    else { DateTimeFormat.forPattern(df) }
  }
}

/** Read single band GeoTiff from S3
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""YYYY:MM:DD HH:MM:SS""
  */
class TemporalGeoTiffS3InputFormat extends S3InputFormat[TemporalProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new TemporalGeoTiffS3RecordReader(context)
}

class TemporalGeoTiffS3RecordReader(context: TaskAttemptContext) extends S3RecordReader[TemporalProjectedExtent, Tile] {
  val timeTag = TemporalGeoTiffS3InputFormat.getTimeTag(context)
  val dateFormatter = TemporalGeoTiffS3InputFormat.getTimeFormatter(context)

  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)

    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val dateTime = DateTime.parse(dateTimeString, dateFormatter)

    //WARNING: Assuming this is a single band GeoTiff
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (TemporalProjectedExtent(extent, crs, dateTime), tile)
  }
}
