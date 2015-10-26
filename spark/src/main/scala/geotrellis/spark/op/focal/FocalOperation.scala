package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import spire.syntax.cfor._

import annotation.tailrec
import scala.reflect.ClassTag

object FocalOperation {

  def apply[K: SpatialComponent: ClassTag](rdd: RDD[(K, Tile)], neighborhood: Neighborhood, opBounds: Option[GridBounds] = None)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RDD[(K, Tile)] = {

    val bounds = opBounds.getOrElse(GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))
    val neighbors: RDD[(K, (K, Tile))] = rdd
      .flatMap { case (key, value) =>
        val SpatialKey(col, row) = key.spatialComponent
        for {
          c <- col - 1 to col + 1
          r <- row - 1 to row + 1
          if !(c == col && r == row) && bounds.contains(c, r)
        } yield (key.updateSpatialComponent(SpatialKey(c, r)), (key, value))
      }

    rdd.cogroup(neighbors)
      .flatMap { case (key, (iterV, iterW)) =>
        iterV.map { tile =>
          val neighbors = iterW.map { case (key, value) => (key.spatialComponent, value) }
          val neighborsSeq = SeqTileNeighbors.fromKeys(key.spatialComponent, neighbors)
          val (neighborhoodTile, analysisArea) = TileWithNeighbors(tile, neighborsSeq.getNeighbors)

          (key, calc(neighborhoodTile, neighborhood, Some(analysisArea)))
        }
      }
  }

  def apply[K: SpatialComponent: ClassTag](rasterRDD: RasterRDD[K], neighborhood: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] = {
    new RasterRDD(
      apply(rasterRDD, neighborhood, Some(rasterRDD.metaData.gridBounds))(calc),
      rasterRDD.metaData)
  }
}

abstract class FocalOperation[K: SpatialComponent: ClassTag] extends RasterRDDMethods[K] {

  def focal(n: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] =
    FocalOperation(rasterRDD, n)(calc)

  def focalWithExtent(n: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds], RasterExtent) => Tile): RasterRDD[K] = {
    val extent = rasterRDD.metaData.layout.rasterExtent
    FocalOperation(rasterRDD, n){ (tile, n, bounds) =>
      calc(tile, n, bounds, extent)
    }
  }
}