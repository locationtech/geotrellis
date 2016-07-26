package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object RDDLayoutMerge {
  /** Merges an RDD with metadata that contains a layout definition into another. */
  def merge[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: (? => LayoutDefinition)
  ](left: RDD[(K, V)] with Metadata[M], right: RDD[(K, V)] with Metadata[M]) = {
    val thisLayout: LayoutDefinition = left.metadata
    val thatLayout: LayoutDefinition = right.metadata

    val cutRdd =
      right
        .flatMap { case (k: K, tile: V) =>
          val extent = thatLayout.mapTransform(k)
          thisLayout.mapTransform(extent)
            .coords
            .map { case (col, row) =>
              val outKey = k.setComponent(SpatialKey(col, row))
              val newTile = tile.prototype(thisLayout.tileCols, thisLayout.tileRows)
              val merged = newTile.merge(thisLayout.mapTransform(outKey), extent, tile)
              (outKey, merged)
            }
        }


    left.withContext { rdd => rdd.merge(cutRdd) }
  }
}
