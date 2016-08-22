package geotrellis.spark.etl

import geotrellis.spark._
import geotrellis.raster.MultibandTile
import geotrellis.spark.util.SparkUtils
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkConf

object MultibandIngest {
  def main(args: Array[String]): Unit = {
    implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL MultibandIngest", new SparkConf(true))
    try {
      Etl.ingest[ProjectedExtent, SpatialKey, MultibandTile](args)
    } finally {
      sc.stop()
    }
  }
}
