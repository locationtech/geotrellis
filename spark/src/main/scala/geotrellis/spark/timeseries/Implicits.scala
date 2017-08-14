package geotrellis.spark.timeseries

import geotrellis.spark._

import org.apache.spark.rdd.RDD


object Implicits extends Implicits

trait Implicits {
  implicit class withRDDTimeSeriesMethods(val self: TileLayerRDD[SpaceTimeKey])
      extends RDDTimeSeriesMethods
}
