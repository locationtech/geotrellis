package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read multi band GeoTiff from S3 */
@deprecated("MultibandGeoTiffS3InputFormat is deprecated, use S3GeoTiffRDD instead", "1.0.0")
class MultibandGeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, MultibandTile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new S3RecordReader[ProjectedExtent, MultibandTile](getS3Client(context)) {
      def read(key: String, bytes: Array[Byte]) = {
        val geoTiff = MultibandGeoTiff(bytes)
        val inputCrs = GeoTiffS3InputFormat.getCrs(context)
        val projectedExtent = ProjectedExtent(geoTiff.extent, inputCrs.getOrElse(geoTiff.crs))
        (projectedExtent, geoTiff.tile)
      }
    }
}
