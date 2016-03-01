package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


// TODO: Handle metadata lens abstraction for layout definition.
class RDDLayoutMergeMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
  M: (? => {def layout: LayoutDefinition})
](val self: RDD[(K, V)] with Metadata[M]) extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {

 def merge(other: RDD[(K, V)] with Metadata[M]) = {
   val thisLayout = self.metadata.layout
   val thatLayout = other.metadata.layout

   val cutRdd =
       other
         .flatMap { case (k: K, tile: V) =>
           val extent = thatLayout.mapTransform(k)
           thisLayout.mapTransform(extent)
             .coords
             .map { spatialComponent =>
             val outKey = k.updateSpatialComponent(spatialComponent)
             val newTile = tile.prototype(thisLayout.tileCols, thisLayout.tileRows)
             val merged = newTile.merge(thisLayout.mapTransform(outKey), extent, tile)
             (outKey, merged)
           }
       }


   self.withContext { rdd => rdd.merge(cutRdd) }
 }

}
