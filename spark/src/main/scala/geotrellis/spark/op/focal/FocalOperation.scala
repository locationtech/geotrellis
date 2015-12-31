package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.spark.buffer._
import geotrellis.raster._
import geotrellis.raster.op.focal._
import geotrellis.raster.mosaic._
import geotrellis.vector._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import spire.syntax.cfor._

import annotation.tailrec
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

object FocalOperation {
  private def mapOverBufferedTiles[K: SpatialComponent: ClassTag](bufferedTiles: RDD[(K, BufferedTile[Tile])], neighborhood: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RDD[(K, Tile)] =
    bufferedTiles
      .mapValues { case BufferedTile(tile, gridBounds) => calc(tile, neighborhood, Some(gridBounds)) }

  def apply[K: SpatialComponent: ClassTag](rdd: RDD[(K, Tile)], neighborhood: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RDD[(K, Tile)] =
    mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](rdd: RDD[(K, Tile)], neighborhood: Neighborhood, layerBounds: GridBounds)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RDD[(K, Tile)] =
    mapOverBufferedTiles(rdd.bufferTiles(neighborhood.extent, layerBounds), neighborhood)(calc)

  def apply[K: SpatialComponent: ClassTag](rasterRDD: RasterRDD[K], neighborhood: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] = {
    new RasterRDD(
      apply(rasterRDD, neighborhood, rasterRDD.metaData.gridBounds)(calc),
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
