package geotrellis.spark.io

import geotrellis.spark._

import scala.reflect.ClassTag

import scala.util.Try

trait Catalog {
  type DriverType[K] <: Driver[K]

  def load[K: Ordering](layerId: LayerId, params: DriverType[K]#Params): Try[RasterRDD[K]] =
    load(layerId, params, FilterSet.EMPTY[K])

  def load[K: ClassTag](layerId: LayerId, params: DriverType[K]#Params, filters: FilterSet[K]): Try[RasterRDD[K]]

  def save[K: ClassTag](layerId: LayerId, rdd: RasterRDD[K], params: DriverType[K]#Params): Try[Unit]
}
