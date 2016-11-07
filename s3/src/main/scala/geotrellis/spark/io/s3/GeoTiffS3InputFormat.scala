package geotrellis.spark.io.s3

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark.io.hadoop._
import geotrellis.vector._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._

object GeoTiffS3InputFormat {
  final val GEOTIFF_CRS = "GEOTIFF_CRS"

  def setCrs(job: Job, crs: CRS): Unit =
    setCrs(job.getConfiguration, crs)

  def setCrs(conf: Configuration, crs: CRS): Unit =
    conf.setSerialized(GEOTIFF_CRS, crs)

  def getCrs(job: JobContext): Option[CRS] =
    job.getConfiguration.getSerializedOption[CRS](GEOTIFF_CRS)
}

/** Read single band GeoTiff from S3 */
class GeoTiffS3InputFormat extends S3InputFormat[ProjectedExtent, Tile] {
  def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new GeoTiffS3RecordReader(getS3Client(context), context)
}

class GeoTiffS3RecordReader(s3Client: S3Client, context: TaskAttemptContext) extends S3RecordReader[ProjectedExtent, Tile](s3Client) {
  def read(key: String, bytes: Array[Byte]) = {
    val geoTiff = SinglebandGeoTiff(bytes)
    val inputCrs = GeoTiffS3InputFormat.getCrs(context)
    val ProjectedRaster(Raster(tile, extent), crs) = geoTiff.projectedRaster
    (ProjectedExtent(extent, inputCrs.getOrElse(crs)), tile)
  }
}
