package geotrellis.spark.io.s3

import com.github.nscala_time.time.Imports._
import geotrellis.proj4._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read single band GeoTiff from S3 */
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = 
    new S3RecordReader[ProjectedExtent, Tile] {
      def read(bytes: Array[Byte]) = {
        val geoTiff = GeoTiffReader.read(bytes)        
        val GeoTiffBand(tile, extent, crs, _) = geoTiff.bands.head
        (ProjectedExtent(extent, crs), tile)        
      }
    }     
}
