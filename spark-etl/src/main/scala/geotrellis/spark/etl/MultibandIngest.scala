package geotrellis.spark.etl

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.raster.MultibandTile
import geotrellis.spark.util.SparkUtils
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkConf

object MultibandIngest extends App {
  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL SinglebandIngest", new SparkConf(true))
  Etl.ingest[ProjectedExtent, GridKey, MultibandTile](args, ZCurveKeyIndexMethod)
  sc.stop()
}
