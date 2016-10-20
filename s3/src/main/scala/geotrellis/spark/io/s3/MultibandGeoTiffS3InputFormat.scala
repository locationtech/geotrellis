package geotrellis.spark.io.s3

import geotrellis.util.StreamByteReader
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import geotrellis.spark.io.s3.util.S3BytesStreamer
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

      def read(key: String, bytes: S3BytesStreamer) =
        read(key, None, bytes)
      
      def read(key: String, e: Extent, bytes: S3BytesStreamer) =
        read(key, Some(e), bytes)

      def read(key: String, e: Option[Extent], bytes: S3BytesStreamer) = {
        val reader = StreamByteReader(bytes)
        val geoTiff = MultibandGeoTiff(reader, e)
        val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
        (ProjectedExtent(extent, crs), tile)
      }
    }
}
