package geotrellis.spark.regrid

import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import scala.reflect._
import scala.collection.mutable

object Regrid {

  def regrid[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileSize: Int): RDD[(K, V)] with Metadata[M] = {
    // println(layer.metadata.toJson.prettyPrint)
    val md = layer.metadata
    val ld = md.getComponent[LayoutDefinition]

    if(ld.tileLayout.tileCols == tileSize) {
      layer
    } else {
      val ceil = { x: Double => math.ceil(x).toInt }

      val tl = ld.tileLayout
      val oldTileSize = tl.tileCols
      if(tileSize > oldTileSize) {
        val r = tileSize / oldTileSize
        val ntl = TileLayout(ceil(tl.layoutCols / r.toDouble), ceil(tl.layoutRows / r.toDouble), tileSize, tileSize)
        val layoutDefinition =
          LayoutDefinition(
            ld.extent,
            ntl
          )

        val bounds =
          md.getComponent[Bounds[K]] match {
            case KeyBounds(minKey, maxKey) =>
              val newMinKey = {
                val SpatialKey(minKeyCol, minKeyRow) = minKey.getComponent[SpatialKey]
                val sk = SpatialKey(minKeyCol / r, minKeyRow / r)
                minKey.setComponent[SpatialKey](sk)
              }
              val newMaxKey = {
                val SpatialKey(maxKeyCol, maxKeyRow) = maxKey.getComponent[SpatialKey]
                val sk = SpatialKey(ceil(maxKeyCol / r.toDouble), ceil(maxKeyRow / r.toDouble))
                maxKey.setComponent[SpatialKey](sk)
              }
              KeyBounds(newMinKey, newMaxKey)
            case EmptyBounds =>
              EmptyBounds
          }

        val newMd =
          md.setComponent[LayoutDefinition](layoutDefinition)
            .setComponent[Bounds[K]](bounds)

        val tiled =
          layer
            .map { case (key, tile) =>
                   val sk = key.getComponent[SpatialKey]
                   val newKey = key.setComponent[SpatialKey](SpatialKey(sk.col / r, sk.row / r))
                                                            (newKey, (key, tile))
                 }
        .groupByKey
        .map { case (key, tiles) =>
               val pieces =
                 tiles.map { case (oldKey, oldTile) =>
                             val SpatialKey(oldCol, oldRow) = oldKey.getComponent[SpatialKey]
                             val updateCol = (oldCol % r) * oldTileSize
                             val updateRow = (oldRow % r) * oldTileSize
                             (oldTile, (updateCol, updateRow))
                           }
               val newTile =
                   implicitly[Stitcher[V]].stitch(pieces, tileSize, tileSize)

               (key, newTile)
             }

        ContextRDD(tiled, newMd)
      } else {
        val r = oldTileSize / tileSize
        val ntl = TileLayout(tl.layoutCols * r, tl.layoutRows * r, tileSize, tileSize)
        val layoutDefinition =
          LayoutDefinition(
            ld.extent,
            ntl
          )

        val bounds =
          md.getComponent[Bounds[K]] match {
            case KeyBounds(minKey, maxKey) =>
              val newMinKey = {
                val SpatialKey(minKeyCol, minKeyRow) = minKey.getComponent[SpatialKey]
                val sk = SpatialKey(minKeyCol * r, minKeyRow * r)
                minKey.setComponent[SpatialKey](sk)
              }
              val newMaxKey = {
                val SpatialKey(maxKeyCol, maxKeyRow) = maxKey.getComponent[SpatialKey]
                val sk = SpatialKey((maxKeyCol + 1) * r - 1, (maxKeyRow + 1) * r - 1)
                maxKey.setComponent[SpatialKey](sk)
              }
              KeyBounds(newMinKey, newMaxKey)
            case EmptyBounds =>
              EmptyBounds
          }

        val newMd =
          md.setComponent[LayoutDefinition](layoutDefinition)
            .setComponent[Bounds[K]](bounds)

        val layoutCols = r
        val layoutRows = r

        val tiled =
          layer.flatMap { case (key, tile) =>
                          val result = mutable.ListBuffer[(K, V)]()
                          val thisKey = key.getComponent[SpatialKey]
                          for(layoutRow <- 0 until layoutRows) {
                            for(layoutCol <- 0 until layoutCols) {
                              val sk = SpatialKey(thisKey.col * layoutCols + layoutCol, thisKey.row * layoutRows + layoutRow)
                              val newTile =
                                tile.crop(
                                  GridBounds(
                                    layoutCol * tileSize,
                                    layoutRow * tileSize,
                                    (layoutCol + 1) * tileSize - 1,
                                    (layoutRow + 1) * tileSize - 1
                                  )
                                )

                              result += ((key.setComponent[SpatialKey](sk), newTile))
                            }
                          }

                          result
                        }

        ContextRDD(tiled, newMd)
      }
    }
  }

}
