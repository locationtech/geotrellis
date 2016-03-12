package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read multi band GeoTiff from S3 */
class MultibandGeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, MultibandTile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[ProjectedExtent, MultibandTile] {
      def read(key: String, bytes: Array[Byte]) = {
        val geoTiff = MultibandGeoTiff(bytes)
        val projectedExtent = ProjectedExtent(geoTiff.extent, geoTiff.crs)
        (projectedExtent, geoTiff.tile)
      }
    }
}
