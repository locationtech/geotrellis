package geotrellis.spark.etl

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.raster.MultiBandTile
import geotrellis.spark.utils.SparkUtils
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkConf

object MultibandIngest extends App {
  implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL SinglebandIngest", new SparkConf(true))
  Etl.ingest[ProjectedExtent, SpatialKey, MultiBandTile](args, ZCurveKeyIndexMethod)
  sc.stop()
}