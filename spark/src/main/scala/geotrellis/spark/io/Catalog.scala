package geotrellis.spark.io

import geotrellis.spark.{FilterSet, Filterable, KeyFilter, RasterRDD}

import scala.reflect.ClassTag

import scala.reflect._
import scala.language.higherKinds
import scala.util.Try

trait Driver[K] {
  /** Driver specific parameter required to save a raster */
  type Params
}

class CatalogError(val message: String) extends Exception(message)

class DriverNotFound[K:ClassTag]
  extends CatalogError(s"Driver not found for key type '${classTag[K]}'")

class LayerNotFound(layer: String, zoom: Int)
  extends CatalogError(s"LayerMetaData not found for layer '$layer' at zoom $zoom")

trait Catalog {
  type DriverType[K] <: Driver[K]

  def register[K:ClassTag](driver: DriverType[K]): Unit

  def load[K:ClassTag](layerName: String, zoom: Int, filters: FilterSet[K]): Try[RasterRDD[K]]

  def save[K:ClassTag](rdd: RasterRDD[K], layer:String, params: DriverType[K]#Params): Try[Unit]
}