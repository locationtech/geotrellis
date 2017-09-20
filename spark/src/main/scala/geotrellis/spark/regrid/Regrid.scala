package geotrellis.spark.regrid

import org.apache.spark.rdd.RDD

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._

import scala.reflect._
import scala.collection.mutable

object Regrid {

  private case class Interval[N : Ordering](start: N, end: N) {
    val ord = implicitly[Ordering[N]]
    // let the interval be start to end, inclusive
    def intersect(that: Interval[N]) = {
        Interval(if (ord.compare(this.start, that.start) > 0) this.start else that.start, 
                 if (ord.compare(this.end, that.end) < 0) this.end else that.end)
    }
  }

  def regrid[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileCols: Int, tileRows: Int): RDD[(K, V)] with Metadata[M] = {
    // println(layer.metadata.toJson.prettyPrint)
    val md = layer.metadata
    val ld = md.getComponent[LayoutDefinition]

    if(ld.tileLayout.tileCols == tileCols && ld.tileLayout.tileRows == tileRows) {
      layer
    } else {
      val ceil = { x: Double => math.ceil(x).toInt }

      val tl = ld.tileLayout
      val oldEx = ld.extent
      val CellSize(cellW, cellH) = ld.cellSize

      val (oldW, oldH) = (tl.tileCols, tl.tileRows)
      val (newW, newH) = (tileCols, tileRows)
      val ntl = TileLayout(ceil(tl.layoutCols * oldW / newW.toDouble), ceil(tl.layoutRows * oldH / newH.toDouble), tileCols, tileRows)

      // Set up a new layout where the upper-left corner of the (0,0) spatial key
      // is preserved, and the right and bottom edges are are adjusted to cover
      // the new extent of the resized tiles
      val nld = LayoutDefinition(
        Extent(
          oldEx.xmin, 
          oldEx.ymax - cellH * tl.layoutRows * oldH, 
          oldEx.xmin + cellW * tl.layoutCols * oldW, 
          oldEx.ymax), 
        ntl)

      val bounds =
        md.getComponent[Bounds[K]] match {
          case KeyBounds(minKey, maxKey) =>
            val newMinKey = {
              val sk = nld.mapTransform(ld.mapTransform(minKey.getComponent[SpatialKey]).northWest)
              minKey.setComponent[SpatialKey](sk)
            }
            val newMaxKey = {
              val sk = nld.mapTransform(ld.mapTransform(maxKey.getComponent[SpatialKey]).southEast)
              maxKey.setComponent[SpatialKey](sk)
            }
            KeyBounds(newMinKey, newMaxKey)
          case EmptyBounds =>
            EmptyBounds
        }

      val newMd =
        md.setComponent[LayoutDefinition](nld)
          .setComponent[Bounds[K]](bounds)

      val tiled: RDD[(K, V)] =
        layer
          .flatMap{ case (key, oldTile) => {
            val SpatialKey(oldX, oldY) = key

            val oldXstart = oldX.toLong * oldW.toLong
            val oldYstart = oldY.toLong * oldH.toLong
            val oldXrange = Interval(oldXstart, oldXstart + oldW - 1)
            val oldYrange = Interval(oldYstart, oldYstart + oldH - 1)

            val tileEx = ld.mapTransform(key)
            val newBounds = nld.mapTransform(tileEx)

            for (
              x <- newBounds.colMin to newBounds.colMax ;
              newXrange = {
                val newXstart = x * tileCols.toLong
                Interval(newXstart, newXstart + tileCols - 1)
              } ;
              y <- newBounds.rowMin to newBounds.rowMax ;
              newYrange = {
                val newYstart = y * tileRows.toLong
                Interval(newYstart, newYstart + tileRows - 1)
              }
            ) yield {
              val newKey = key.setComponent[SpatialKey](SpatialKey(x, y))
              val xSpan: Interval[Long] = oldXrange intersect newXrange
              val ySpan: Interval[Long] = oldYrange intersect newYrange
              newKey -> 
                (oldTile.crop((xSpan.start - oldXstart).toInt, 
                              (ySpan.start - oldYstart).toInt, 
                              (xSpan.end - oldXstart).toInt, 
                              (ySpan.end - oldYstart).toInt),
                 ((xSpan.start - newXrange.start).toInt, (ySpan.start - newYrange.start).toInt)
                )
            }
          }}
          .groupByKey
          .mapValues { tiles => implicitly[Stitcher[V]].stitch(tiles, tileCols, tileRows) }

      ContextRDD(tiled, newMd)

      // val ceil = { x: Double => math.ceil(x).toInt }

      // val tl = ld.tileLayout
      // val oldTileCols = tl.tileCols
      // val oldTileRows = tl.tileRows
      // if(tileSize > oldTileSize) {
      //   val r = tileSize / oldTileSize
      //   val ntl = TileLayout(ceil(tl.layoutCols / r.toDouble), ceil(tl.layoutRows / r.toDouble), tileSize, tileSize)
      //   val layoutDefinition =
      //     LayoutDefinition(
      //       ld.extent,
      //       ntl
      //     )

      //   val bounds =
      //     md.getComponent[Bounds[K]] match {
      //       case KeyBounds(minKey, maxKey) =>
      //         val newMinKey = {
      //           val SpatialKey(minKeyCol, minKeyRow) = minKey.getComponent[SpatialKey]
      //           val sk = SpatialKey(minKeyCol / r, minKeyRow / r)
      //           minKey.setComponent[SpatialKey](sk)
      //         }
      //         val newMaxKey = {
      //           val SpatialKey(maxKeyCol, maxKeyRow) = maxKey.getComponent[SpatialKey]
      //           val sk = SpatialKey(ceil(maxKeyCol / r.toDouble), ceil(maxKeyRow / r.toDouble))
      //           maxKey.setComponent[SpatialKey](sk)
      //         }
      //         KeyBounds(newMinKey, newMaxKey)
      //       case EmptyBounds =>
      //         EmptyBounds
      //     }

      //   val newMd =
      //     md.setComponent[LayoutDefinition](layoutDefinition)
      //       .setComponent[Bounds[K]](bounds)

      //   val tiled =
      //     layer
      //       .map { case (key, tile) =>
      //              val sk = key.getComponent[SpatialKey]
      //              val newKey = key.setComponent[SpatialKey](SpatialKey(sk.col / r, sk.row / r))
      //                                                       (newKey, (key, tile))
      //            }
      //   .groupByKey
      //   .map { case (key, tiles) =>
      //          val pieces =
      //            tiles.map { case (oldKey, oldTile) =>
      //                        val SpatialKey(oldCol, oldRow) = oldKey.getComponent[SpatialKey]
      //                        val updateCol = (oldCol % r) * oldTileSize
      //                        val updateRow = (oldRow % r) * oldTileSize
      //                        (oldTile, (updateCol, updateRow))
      //                      }
      //          val newTile =
      //              implicitly[Stitcher[V]].stitch(pieces, tileSize, tileSize)

      //          (key, newTile)
      //        }

      //   ContextRDD(tiled, newMd)
      // } else {
      //   val r = oldTileSize / tileSize
      //   val ntl = TileLayout(tl.layoutCols * r, tl.layoutRows * r, tileSize, tileSize)
      //   val layoutDefinition =
      //     LayoutDefinition(
      //       ld.extent,
      //       ntl
      //     )

      //   val bounds =
      //     md.getComponent[Bounds[K]] match {
      //       case KeyBounds(minKey, maxKey) =>
      //         val newMinKey = {
      //           val SpatialKey(minKeyCol, minKeyRow) = minKey.getComponent[SpatialKey]
      //           val sk = SpatialKey(minKeyCol * r, minKeyRow * r)
      //           minKey.setComponent[SpatialKey](sk)
      //         }
      //         val newMaxKey = {
      //           val SpatialKey(maxKeyCol, maxKeyRow) = maxKey.getComponent[SpatialKey]
      //           val sk = SpatialKey((maxKeyCol + 1) * r - 1, (maxKeyRow + 1) * r - 1)
      //           maxKey.setComponent[SpatialKey](sk)
      //         }
      //         KeyBounds(newMinKey, newMaxKey)
      //       case EmptyBounds =>
      //         EmptyBounds
      //     }

      //   val newMd =
      //     md.setComponent[LayoutDefinition](layoutDefinition)
      //       .setComponent[Bounds[K]](bounds)

      //   val layoutCols = r
      //   val layoutRows = r

      //   val tiled =
      //     layer.flatMap { case (key, tile) =>
      //                     val result = mutable.ListBuffer[(K, V)]()
      //                     val thisKey = key.getComponent[SpatialKey]
      //                     for(layoutRow <- 0 until layoutRows) {
      //                       for(layoutCol <- 0 until layoutCols) {
      //                         val sk = SpatialKey(thisKey.col * layoutCols + layoutCol, thisKey.row * layoutRows + layoutRow)
      //                         val newTile =
      //                           tile.crop(
      //                             GridBounds(
      //                               layoutCol * tileSize,
      //                               layoutRow * tileSize,
      //                               (layoutCol + 1) * tileSize - 1,
      //                               (layoutRow + 1) * tileSize - 1
      //                             )
      //                           )

      //                         result += ((key.setComponent[SpatialKey](sk), newTile))
      //                       }
      //                     }

      //                     result
      //                   }

      //   ContextRDD(tiled, newMd)
      // }
    }
  }

  def regrid[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileSize: Int): RDD[(K, V)] with Metadata[M] = regrid(layer, tileSize, tileSize)

}
