package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.io.s3._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalGeoTiffS3Input extends S3Input[TemporalProjectedExtent, Tile] {
  val format = "temporal-geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    val path = getPath(conf.input.backend)
    S3GeoTiffRDD.temporal(path.bucket, path.prefix, S3GeoTiffRDD.Options(
      timeTag = conf.output.keyIndexMethod.timeTag.getOrElse(S3GeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT),
      timeFormat = conf.output.keyIndexMethod.timeTag.getOrElse(S3GeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT),
      crs = conf.input.getCrs
    ))
  }
}
