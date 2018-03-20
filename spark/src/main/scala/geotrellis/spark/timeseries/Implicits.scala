package geotrellis.spark.timeseries

import geotrellis.spark._
import geotrellis.tiling._

import org.apache.spark.rdd.RDD


object Implicits extends Implicits

trait Implicits {
  implicit class withRDDTimeSeriesMethods(val self: TileLayerRDD[SpaceTimeKey])
      extends RDDTimeSeriesMethods
}
