package geotrellis.spark.mapalgebra

import scala.reflect.ClassTag
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions

import org.apache.spark.sql.Dataset

trait TileDatasetMethods[K] extends MethodExtensions[Dataset[(K, Tile)]] {
  implicit val keyClassTag: ClassTag[K]
}

