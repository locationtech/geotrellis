package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.local._

trait LocalRasterRDDSeqMethods[K] extends RasterRDDSeqMethods[K] {

  private def r(f: Seq[(K, Tile)] => (K, Tile)): RasterRDD[K] =
    rasterRDDs.headOption match {
      case Some(head) => head.combinePairs(rasterRDDs.tail.toSeq)(f)
      case None => sys.error("raster rdd operations can't be applied to empty seq!")
    }

  def localAdd: RasterRDD[K] = r {
    case tiles => (tiles.head.id, Add(tiles.map(_.tile)))
  }

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety: RasterRDD[K] = r {
    case tiles => (tiles.head.id, Variety(tiles.map(_.tile)))
  }

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean: RasterRDD[K] = r {
    case tiles => (tiles.head.id, Mean(tiles.map(_.tile)))
  }

  def localMin: RasterRDD[K] = r {
    case tiles => (tiles.head.id, Min(tiles.map(_.tile)))
  }

  def localMinN(n: Int): RasterRDD[K] = r {
    case tiles => (tiles.head.id, MinN(n, tiles.map(_.tile)))
  }

  def localMax: RasterRDD[K] = r {
    case tiles => (tiles.head.id, Max(tiles.map(_.tile)))
  }

  def localMaxN(n: Int): RasterRDD[K] = r {
    case tiles => (tiles.head.id, MaxN(n, tiles.map(_.tile)))
  }

  def localMinority(n: Int = 0): RasterRDD[K] = r {
    case tiles => (tiles.head.id, Minority(n, tiles.map(_.tile)))
  }

  def localMajority(n: Int = 0): RasterRDD[K] = r {
    case tiles => (tiles.head.id, Majority(n, tiles.map(_.tile)))
  }
}
