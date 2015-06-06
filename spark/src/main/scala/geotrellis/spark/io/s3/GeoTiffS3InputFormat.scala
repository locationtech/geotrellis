package geotrellis.spark.io.s3

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read single band GeoTiff from S3 */
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = 
    new S3RecordReader[ProjectedExtent, Tile] {
      def read(bytes: Array[Byte]) = {
        val geoTiff = SingleBandGeoTiff(bytes)        
        val ProjectedRaster(tile, extent, crs) = geoTiff.projectedRaster
        (ProjectedExtent(extent, crs), tile)        
      }
    }     
}
