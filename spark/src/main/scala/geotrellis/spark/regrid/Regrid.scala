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
    assert(ord.compare(start, end) < 1)
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
          oldEx.ymax - cellH * ntl.layoutRows * tileRows,
          oldEx.xmin + cellW * ntl.layoutCols * tileCols,
          oldEx.ymax),
        ntl)
      // println(s"Created new LayoutDefinition with ${nld.extent} and $ntl from ${ld.extent} and ${ld.tileLayout}")

      val bounds =
        md.getComponent[Bounds[K]] match {
          case KeyBounds(minKey, maxKey) =>
            val ulSK = minKey.getComponent[SpatialKey]
            val lrSK = maxKey.getComponent[SpatialKey]
            val pxXrange = Interval(ulSK._1 * oldW.toLong, (lrSK._1 + 1) * oldW.toLong - 1)
            val pxYrange = Interval(ulSK._2 * oldH.toLong, (lrSK._2 + 1) * oldH.toLong - 1)

            val newMinKey = SpatialKey((pxXrange.start / tileCols).toInt, (pxYrange.start / tileRows).toInt)
            val newMaxKey = SpatialKey((pxXrange.end / tileCols).toInt, (pxYrange.end / tileRows).toInt)

            // println(s"In bounds calculation: $minKey became $newMinKey and $maxKey became $newMaxKey")
            KeyBounds(minKey.setComponent[SpatialKey](newMinKey), maxKey.setComponent[SpatialKey](newMaxKey))
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

            val xBounds = (oldXstart / tileCols).toInt to (oldXrange.end.toDouble / tileCols).toInt
            val yBounds = (oldYstart / tileRows).toInt to (oldYrange.end.toDouble / tileRows).toInt

            // println(s"For $key:\n\tIntersects $newBounds in new layout (x: $xBounds, y: $yBounds) (pixel ranges, x=$oldXrange, y=$oldYrange)")

            for (
              // x <- newBounds.colMin to newBounds.colMax ;
              x <- xBounds ;
              newXrange = {
                val newXstart = x * tileCols.toLong
                Interval(newXstart, newXstart + tileCols - 1)
              } ;
              // y <- newBounds.rowMin to newBounds.rowMax ;
              y <- yBounds ;
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
    }
  }

  def regrid[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileSize: Int): RDD[(K, V)] with Metadata[M] = regrid(layer, tileSize, tileSize)

}
