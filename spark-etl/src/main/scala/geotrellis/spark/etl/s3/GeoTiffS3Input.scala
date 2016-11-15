package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.s3.S3GeoTiffRDD
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffS3Input extends S3Input[ProjectedExtent, Tile] {
  val format = "geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val path = getPath(conf.input.backend)
    S3GeoTiffRDD.spatial(path.bucket, path.prefix, S3GeoTiffRDD.Options(
      crs = conf.input.getCrs
    ))
  }
}

