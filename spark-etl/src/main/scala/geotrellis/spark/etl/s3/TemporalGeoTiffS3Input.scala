package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.ingest._
import geotrellis.spark.io.s3._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalGeoTiffS3Input extends S3Input[TemporalProjectedExtent, Tile] {
  val format = "temporal-geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    val hadoopConfig = configuration(conf.input)
    conf.output.keyIndexMethod.timeTag.foreach(TemporalGeoTiffS3InputFormat.setTimeTag(hadoopConfig, _))
    conf.output.keyIndexMethod.timeFormat.foreach(TemporalGeoTiffS3InputFormat.setTimeFormat(hadoopConfig, _))
    conf.input.crs.foreach(TemporalGeoTiffS3InputFormat.setCrs(hadoopConfig, _))
    sc.newAPIHadoopRDD(hadoopConfig, classOf[TemporalGeoTiffS3InputFormat], classOf[TemporalProjectedExtent], classOf[Tile])
  }
}
