package geotrellis.spark.ingest

import geotrellis.raster.mosaic.MergeView
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import org.apache.spark.rdd.RDD


trait MergeMethods[T] {
  def merge(other: T): T
}

/**
 * Merges tiles of other RDD into this RDD using cogroup.
 */
class RddMergeMethods[K, TileType: MergeView](
  rdd: RDD[(K, TileType)])
extends MergeMethods[RDD[(K, TileType)]]{
  def merge(other: RDD[(K, TileType)]): RDD[(K, TileType)] = {
    val fMerge = (_: TileType).merge(_: TileType)
    rdd
      .cogroup(other)
      .map { case (key, (myTiles, otherTiles)) =>
        if (myTiles.nonEmpty && otherTiles.nonEmpty) {
          val a = myTiles.reduce(fMerge)
          val b = otherTiles.reduce(fMerge)
          (key, fMerge(a, b))
        } else if (myTiles.nonEmpty) {
          (key, myTiles.reduce(fMerge))
        } else {
          (key, otherTiles.reduce(fMerge))
        }
      }
  }
}

/**
 * Cuts up the other RDD into tiles that match the layout of this RDD and merges them.
 */
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