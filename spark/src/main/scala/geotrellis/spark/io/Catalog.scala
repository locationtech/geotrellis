package geotrellis.spark.io

import geotrellis.spark.{FilterSet, Filterable, KeyFilter, RasterRDD}

import scala.reflect.ClassTag

import scala.reflect._
import scala.language.higherKinds

trait Driver[K] {
  /** Driver specific parameter required to save a raster */
  type Params
}

trait Catalog {
  type DriverType[K] <: Driver[K]

  def register[K:ClassTag](driver: DriverType[K]): Unit

  def load[K:ClassTag](layerName: String, zoom: Int, filters: FilterSet[K]): Option[RasterRDD[K]]

  def save[K:ClassTag](rdd: RasterRDD[K], layer:String, params: DriverType[K]#Params)
}