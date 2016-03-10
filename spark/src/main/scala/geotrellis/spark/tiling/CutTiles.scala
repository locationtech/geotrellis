package geotrellis.spark.tiling

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample._
import geotrellis.spark._

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object CutTiles {
  def apply[
    K1: (? => TilerKeyMethods[K1, K2]),
    K2: GridComponent: ClassTag,
    V <: CellGrid: ClassTag: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ] (
    rdd: RDD[(K1, V)],
    cellType: CellType,
    layoutDefinition: LayoutDefinition,
    resampleMethod: ResampleMethod = NearestNeighbor
  ): RDD[(K2, V)] =
    rdd
      .flatMap { tup =>
        val (inKey, tile) = tup
        val extent = inKey.extent
        val mapTransform = layoutDefinition.mapTransform
        val (tileCols, tileRows) = layoutDefinition.tileLayout.tileDimensions
        mapTransform(extent)
          .coords
          .map  { spatialComponent =>
            val outKey = inKey.translate(spatialComponent)
            val newTile = tile.prototype(cellType, tileCols, tileRows)
            (outKey, newTile.merge(mapTransform(outKey), extent, tile))
          }
      }
}
