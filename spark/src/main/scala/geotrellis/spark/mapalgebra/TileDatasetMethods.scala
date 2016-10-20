package geotrellis.spark.mapalgebra

import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions

import org.apache.spark.sql.Dataset

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

trait TileDatasetMethods[K] extends MethodExtensions[Dataset[(K, Tile)]] {
  implicit val keyTypeTag: TypeTag[K]
  implicit val keyClassTag: ClassTag[K]
}

