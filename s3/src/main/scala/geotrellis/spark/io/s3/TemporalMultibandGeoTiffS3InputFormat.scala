package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import org.apache.hadoop.mapreduce._
import org.joda.time._


/** Read single band GeoTiff from S3
  *
  * This can be configured with the hadoop configuration by providing:
  * TemporalMultibandGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalMultibandGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""YYYY:MM:DD HH:MM:SS""
  */
class TemporalMultibandGeoTiffS3InputFormat extends S3InputFormat[TemporalProjectedExtent, MultibandTile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[TemporalProjectedExtent, MultibandTile] {
      def read(key: String, bytes: Array[Byte]) = {
        val geoTiff = MultibandGeoTiff(bytes)

        val timeTag = TemporalGeoTiffS3InputFormat.getTimeTag(context)
        val dateFormatter = TemporalGeoTiffS3InputFormat.getTimeFormatter(context)

        val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
        val dateTime = DateTime.parse(dateTimeString, dateFormatter)

        val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
        (TemporalProjectedExtent(extent, crs, dateTime), tile)
      }
    }
}
