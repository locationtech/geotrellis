package geotrellis.spark.mosaic

import scala.reflect.ClassTag

import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.mosaic._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition

class RddLayoutMergeMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: MergeView: ClassTag: (? => TilePrototypeMethods[V]),
  M: (? => {def layout: LayoutDefinition})
](rdd: RDD[(K, V)] with Metadata[M]) extends MergeMethods[RDD[(K, V)] with Metadata[M]] {

 def merge(other: RDD[(K, V)] with Metadata[M]) = {
   val thisLayout = rdd.metadata.layout
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


   rdd.withContext { rdd => rdd.merge(cutRdd) }
 }

}
