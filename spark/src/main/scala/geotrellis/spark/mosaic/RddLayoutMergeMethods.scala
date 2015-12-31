package geotrellis.spark.mosaic

import scala.reflect.ClassTag

import org.apache.spark.rdd.RDD

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark._
import geotrellis.spark.ingest.CellGridPrototypeView
import geotrellis.spark.tiling.LayoutDefinition

class RddLayoutMergeMethods[K: SpatialComponent: ClassTag, TileType: MergeView: CellGridPrototypeView: ClassTag, M: (? => {def layout: LayoutDefinition})](
 rdd: (RDD[(K, TileType)] with Metadata[M])
) extends MergeMethods[RDD[(K, TileType)] with Metadata[M]] {

 def merge(other: RDD[(K, TileType)] with Metadata[M]) = {
   val thisLayout = rdd.metadata.layout
   val thatLayout = other.metadata.layout

   val cutRdd = 
       other
         .flatMap { case (k: K, tile: TileType) =>
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
