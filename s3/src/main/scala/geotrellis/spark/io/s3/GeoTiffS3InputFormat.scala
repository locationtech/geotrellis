package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read single band GeoTiff from S3 */
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new GeoTiffS3RecordReader
}

class GeoTiffS3RecordReader extends S3RecordReader[ProjectedExtent, Tile] {
  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (ProjectedExtent(extent, crs), tile)
  }
}
