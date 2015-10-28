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

    rdd
      .flatMap { case record @ (key, tile) =>
        val SpatialKey(col, row) = key.spatialComponent
        for {
          c <- - 1 to + 1
          r <- - 1 to + 1
          if bounds.contains(col + c, row + r)
        } yield {
          val contributesToKey = key.updateSpatialComponent(SpatialKey(col + c,  row + r))
          (contributesToKey, ((-c, -r), tile))
        }
      }
      .groupByKey
      .flatMap { case (key, neighbors) =>        
        TileWithNeighbors.fromOffsets(neighbors)
          .map { case (neighborhoodTile, analysisArea) =>
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