package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.ingest._
import geotrellis.spark.io.s3._
import geotrellis.spark._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalGeoTiffS3Input extends S3Input[SpaceTimeInputKey, SpaceTimeKey] {
  val format = "temporal-geotiff"

  def source(props: Parameters)(implicit sc: SparkContext): RDD[(SpaceTimeInputKey, V)] =
    sc.newAPIHadoopRDD(configuration(props), classOf[TemporalGeoTiffS3InputFormat], classOf[SpaceTimeInputKey], classOf[Tile])
}
