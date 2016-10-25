package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}

/** Read single band GeoTiff from S3 */
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new GeoTiffS3RecordReader(context)
}

class GeoTiffS3RecordReader(context: TaskAttemptContext) extends S3RecordReader[ProjectedExtent, Tile] {
  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)
    val inputCrs = TemporalGeoTiffS3InputFormat.getCrs(context)
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (ProjectedExtent(extent, inputCrs.getOrElse(crs)), tile)
  }
}
