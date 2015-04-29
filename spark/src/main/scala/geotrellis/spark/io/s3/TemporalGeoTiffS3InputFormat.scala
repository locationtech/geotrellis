package geotrellis.spark.io.s3

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector.Extent
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read single band GeoTiff from S3 
 * Input GeoTiffs should have 'ISO_TIME' tag with ISO 8601 DateTime formated timestamp.
 */
class TemporalGeoTiffS3InputFormat extends S3InputFormat[SpaceTimeInputKey,Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = 
    new S3RecordReader[SpaceTimeInputKey,Tile] {
      def read(bytes: Array[Byte]) = {        
        val geoTiff = SingleBandGeoTiff(bytes)

        val isoString = geoTiff.tags("ISO_TIME")
        val dateTime = DateTime.parse(isoString)

        //WARNING: Assuming this is a single band GeoTiff
        val ProjectedRaster(tile, extent, crs) = geoTiff.projectedRaster
        (SpaceTimeInputKey(extent, crs, dateTime), tile)        
      }
    }     
}
