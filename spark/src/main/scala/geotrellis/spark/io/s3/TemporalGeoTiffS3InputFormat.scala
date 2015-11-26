package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector.Extent
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

object TemporalGeoTiffS3InputFormat {
  final val GEOTIFF_TIME_TAG = "GEOTIFF_TIME_TAG"
  final val GEOTIFF_TIME_FORMAT = "GEOTIFF_TIME_FORMAT"
}

/** Read single band GeoTiff from S3 
  * Input GeoTiffs should have 'ISO_TIME' tag with ISO 8601 DateTime formated timestamp.
  * 
  * This can be configured with the hadoop configuration by providing:
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG; default of "TIFFTAG_DATETIME"
  * TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT; default is ""YYYY:MM:DD HH:MM:SS""
  */
class TemporalGeoTiffS3InputFormat extends S3InputFormat[SpaceTimeInputKey,Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = 
    new S3RecordReader[SpaceTimeInputKey,Tile] {
      def read(bytes: Array[Byte]) = {        
        val geoTiff = SingleBandGeoTiff(bytes)

        val conf = context.getConfiguration
        val timeTag = conf.get(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_TAG, "TIFFTAG_DATETIME")
        val dateFormatter = {
          val df = conf.get(TemporalGeoTiffS3InputFormat.GEOTIFF_TIME_FORMAT)
          if(df == null) { DateTimeFormat.forPattern("YYYY:MM:dd HH:mm:ss") }
          else { DateTimeFormat.forPattern(df) }
        }

        val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
        val dateTime = DateTime.parse(dateTimeString, dateFormatter)

        //WARNING: Assuming this is a single band GeoTiff
        val ProjectedRaster(tile, extent, crs) = geoTiff.projectedRaster
        (SpaceTimeInputKey(extent, crs, dateTime), tile)        
      }
    }     
}
