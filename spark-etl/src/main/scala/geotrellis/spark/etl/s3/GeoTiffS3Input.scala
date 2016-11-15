package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.ingest._
import geotrellis.spark.io.s3.{GeoTiffS3InputFormat, TemporalGeoTiffS3InputFormat}
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class GeoTiffS3Input extends S3Input[ProjectedExtent, Tile] {
  val format = "geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val hadoopConfig = configuration(conf.input)
    conf.input.crs.foreach(TemporalGeoTiffS3InputFormat.setCrs(hadoopConfig, _))
    sc.newAPIHadoopRDD(hadoopConfig, classOf[GeoTiffS3InputFormat], classOf[ProjectedExtent], classOf[Tile])
  }
}

