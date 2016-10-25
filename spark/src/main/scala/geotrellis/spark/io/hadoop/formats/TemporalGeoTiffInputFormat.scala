package geotrellis.spark.io.hadoop.formats

import java.time.format.DateTimeFormatter

import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import geotrellis.proj4.CRS

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import java.time.{ZoneOffset, ZonedDateTime}

object TemporalGeoTiffInputFormat {
  final val GEOTIFF_TIME_TAG = "GEOTIFF_TIME_TAG"
  final val GEOTIFF_TIME_TAG_DEFAULT = "TIFFTAG_DATETIME"
  final val GEOTIFF_TIME_FORMAT = "GEOTIFF_TIME_FORMAT"
  final val GEOTIFF_CRS = "GEOTIFF_CRS"
  final val GEOTIFF_TIME_FORMAT_DEFAULT = "yyyy:MM:dd HH:mm:ss"

  def setTimeTag(job: JobContext, timeTag: String): Unit =
    setTimeTag(job.getConfiguration, timeTag)

  def setTimeTag(conf: Configuration, timeTag: String): Unit =
    conf.set(GEOTIFF_TIME_TAG, timeTag)

  def setTimeFormat(job: JobContext, timeFormat: String): Unit =
    setTimeFormat(job.getConfiguration, timeFormat)

  def setTimeFormat(conf: Configuration, timeFormat: String): Unit =
    conf.set(GEOTIFF_TIME_FORMAT, timeFormat)

  def setCrs(conf: Configuration, name: String): Unit = conf.set(GEOTIFF_CRS, name)

  def getTimeTag(job: JobContext) =
    job.getConfiguration.get(GEOTIFF_TIME_TAG, GEOTIFF_TIME_TAG_DEFAULT)

  def getTimeFormatter(job: JobContext): DateTimeFormatter = {
    val df = job.getConfiguration.get(GEOTIFF_TIME_FORMAT)
    (if(df == null) { DateTimeFormatter.ofPattern(GEOTIFF_TIME_FORMAT_DEFAULT) }
    else { DateTimeFormatter.ofPattern(df) }).withZone(ZoneOffset.UTC)
  }

  def getCrs(job: JobContext): Option[CRS] = {
    val name = job.getConfiguration.get(GEOTIFF_CRS, "")
    if(name == "") None else Some(CRS.fromName(name))
  }
}

/** Read single band GeoTiff with a timestamp
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""yyyy:MM:DD HH:MM:SS""
  */
class TemporalGeoTiffInputFormat extends BinaryFileInputFormat[TemporalProjectedExtent, Tile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (TemporalProjectedExtent, Tile) = {
    val geoTiff = SinglebandGeoTiff(bytes)

    val timeTag = TemporalGeoTiffInputFormat.getTimeTag(context)
    val dateFormatter = TemporalGeoTiffInputFormat.getTimeFormatter(context)
    val inputCrs = TemporalGeoTiffInputFormat.getCrs(context)

    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val dateTime = ZonedDateTime.from(dateFormatter.parse(dateTimeString))

    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (TemporalProjectedExtent(extent, inputCrs.getOrElse(crs), dateTime), tile)
  }
}
