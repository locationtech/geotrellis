package geotrellis.spark.io

import geotrellis.spark.{FilterSet, Filterable, KeyFilter, RasterRDD}

import scala.reflect.ClassTag

import scala.reflect._
import scala.util.Try

trait Catalog {
  type DriverType[K] <: Driver[K]

  def register[K:ClassTag](driver: DriverType[K]): Unit

  def load[K:ClassTag](layerId: LayerId, params: DriverType[K]#Params, filters: FilterSet[K] = FilterSet.EMPTY[K]): Try[RasterRDD[K]]

  def save[K:ClassTag](rdd: RasterRDD[K], layerName: String, params: DriverType[K]#Params): Try[Unit]
}
