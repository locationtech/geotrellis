package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class RDDLayoutMergeMethods[
  K: GridComponent: ClassTag,
  V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
  M: (? => LayoutDefinition)
](val self: RDD[(K, V)] with Metadata[M]) extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {

 def merge(other: RDD[(K, V)] with Metadata[M]) = {
   val thisLayout: LayoutDefinition = self.metadata
   val thatLayout: LayoutDefinition = other.metadata

   val cutRdd =
       other
         .flatMap { case (k: K, tile: V) =>
           val extent = thatLayout.mapTransform(k)
           thisLayout.mapTransform(extent)
             .coords
             .map { case (col, row) =>
             val outKey = k.setComponent(GridKey(col, row))
             val newTile = tile.prototype(thisLayout.tileCols, thisLayout.tileRows)
             val merged = newTile.merge(thisLayout.mapTransform(outKey), extent, tile)
             (outKey, merged)
           }
       }


   self.withContext { rdd => rdd.merge(cutRdd) }
 }

}
