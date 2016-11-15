package geotrellis.spark.etl.s3

import geotrellis.proj4.CRS
import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.s3._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalMultibandGeoTiffS3Input extends S3Input[TemporalProjectedExtent, MultibandTile] {
  val format = "temporal-geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val path = getPath(conf.input.backend)
    S3GeoTiffRDD.temporalMultiband(path.bucket, path.prefix, S3GeoTiffRDD.Options(
      timeTag = conf.output.keyIndexMethod.timeTag.getOrElse(S3GeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT),
      timeFormat = conf.output.keyIndexMethod.timeTag.getOrElse(S3GeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT),
      crs = conf.input.crs.map(CRS.fromName)
    ))
  }
}
