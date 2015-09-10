package geotrellis.spark.mosaic


import geotrellis.raster.mosaic.MergeView
import geotrellis.spark._
import geotrellis.spark.ingest.CellGridPrototypeView
import geotrellis.spark.tiling.LayoutDefinition
import org.apache.spark.rdd.RDD

class RddLayoutMergeMethods[K: SpatialComponent, TileType: MergeView: CellGridPrototypeView](
 rdd: (RDD[(K, TileType)], LayoutDefinition))
extends MergeMethods[(RDD[(K, TileType)], LayoutDefinition)] {

 def merge(other: (RDD[(K, TileType)], LayoutDefinition)) = {
   val (thisRdd, thisLayout) = rdd
   val (thatRdd, thatLayout) = other

   val cutRdd = thatRdd
     .flatMap { case(k, tile) =>
       val extent = thatLayout.mapTransform(k)
       thisLayout.mapTransform(extent)
         .coords
         .map { spatialComponent =>
           val outKey = k.updateSpatialComponent(spatialComponent)
           val newTile = tile.prototype(thisLayout.tileCols, thisLayout.tileRows)
           newTile.merge(thisLayout.mapTransform(outKey), extent, tile)
           (outKey, newTile)
         }
       }

   (thisRdd.merge(cutRdd), thisLayout)
 }

}
