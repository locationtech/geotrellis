package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop._
import geotrellis.spark.ingest._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import org.joda.time._
import org.joda.time.format._

/** Read multiband GeoTiff with a timestamp
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""YYYY:MM:DD HH:MM:SS""
  */
class TemporalMultibandGeoTiffInputFormat extends BinaryFileInputFormat[TemporalProjectedExtent, MultibandTile] {
  def read(bytes: Array[Byte], context: TaskAttemptContext): (TemporalProjectedExtent, MultibandTile) = {
    val geoTiff = MultibandGeoTiff(bytes)

    val timeTag = TemporalGeoTiffInputFormat.getTimeTag(context)
    val dateFormatter = TemporalGeoTiffInputFormat.getTimeFormatter(context)

    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val dateTime = DateTime.parse(dateTimeString, dateFormatter)

    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (TemporalProjectedExtent(extent, crs, dateTime), tile)
  }
}