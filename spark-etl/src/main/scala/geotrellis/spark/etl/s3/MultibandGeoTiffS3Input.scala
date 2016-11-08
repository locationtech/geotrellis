package geotrellis.spark.etl.s3

import geotrellis.proj4.CRS
import geotrellis.raster.MultibandTile
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.s3.{GeoTiffS3InputFormat, MultibandGeoTiffS3InputFormat, TemporalGeoTiffS3InputFormat}
import geotrellis.vector.ProjectedExtent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class MultibandGeoTiffS3Input extends S3Input[ProjectedExtent, MultibandTile] {
  val format = "multiband-geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    val hadoopConfig = configuration(conf.input)
    conf.input.crs.foreach(x => GeoTiffS3InputFormat.setCrs(hadoopConfig, CRS.fromName(x)))
    sc.newAPIHadoopRDD(
      hadoopConfig, classOf[MultibandGeoTiffS3InputFormat], classOf[ProjectedExtent], classOf[MultibandTile]
    )
  }
}
