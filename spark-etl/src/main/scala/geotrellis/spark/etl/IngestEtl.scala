package geotrellis.spark.etl

import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.spark.utils.SparkUtils
import geotrellis.raster.CellGrid

import org.apache.spark.SparkConf

import scala.reflect.runtime.universe._

trait IngestEtl[V <: CellGrid] {
  def apply[
    I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: TypeTag
  ](args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules): Etl[I, K, V]

  def ingest[
    I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: TypeTag
  ](args: Seq[String], keyIndexMethod: KeyIndexMethod[K], modules: Seq[TypedModule] = Etl.defaultModules) = {
    val etl = apply[I, K](args, modules)
    implicit val sc = SparkUtils.createSparkContext("GeoTrellis ETL", new SparkConf(true))
    val (zoom, tiledRdd) = etl.tile(etl.reproject(etl.load()))
    etl.save(LayerId(etl.conf.layerName(), zoom), tiledRdd, keyIndexMethod)
    sc.stop()
  }
}
