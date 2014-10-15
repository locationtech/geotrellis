package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

import scala.util.Try

trait Catalog {
  type DriverType[K] <: Driver[K]

  def register[K: ClassTag](driver: DriverType[K]): Unit

  def load[K: ClassTag](layerId: LayerId, params: DriverType[K]#Params, filters: FilterSet[K] = FilterSet.EMPTY[K]): Try[RasterRDD[K]]

  def save[K: ClassTag](layerId: LayerId, rdd: RasterRDD[K], params: DriverType[K]#Params): Try[Unit]
}
