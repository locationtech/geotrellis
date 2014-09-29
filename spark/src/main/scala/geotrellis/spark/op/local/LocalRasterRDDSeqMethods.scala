package geotrellis.spark.op.local

import geotrellis.raster.op.local._

import geotrellis.spark._
import geotrellis.spark.RasterRDDSeqMethods
import geotrellis.spark.rdd.RasterRDD

trait LocalRasterRDDSeqMethods extends RasterRDDSeqMethods {

  private def r(f: Seq[TmsTile] => TmsTile): RasterRDD =
    rasterRDDs.headOption match {
      case Some(head) => head.combineTiles(rasterRDDs.tail.toSeq)(f)
      case None => sys.error("raster rdd operations can't be applied to empty seq!")
    }

  def localAdd: RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Add(tiles.map(_.tile)))
  }

  /** Gives the count of unique values at each location in a set of Tiles.*/
  def localVariety: RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Variety(tiles.map(_.tile)))
  }

  /** Takes the mean of the values of each cell in the set of rasters. */
  def localMean: RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Mean(tiles.map(_.tile)))
  }

  def localMin: RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Min(tiles.map(_.tile)))
  }

  def localMinN(n: Int): RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, MinN(n, tiles.map(_.tile)))
  }

  def localMax: RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Max(tiles.map(_.tile)))
  }

  def localMaxN(n: Int): RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, MaxN(n, tiles.map(_.tile)))
  }

  def localMinority(n: Int = 0): RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Minority(n, tiles.map(_.tile)))
  }

  def localMajority(n: Int = 0): RasterRDD = r {
    case tiles: Seq[TmsTile] => TmsTile(tiles.head.id, Majority(n, tiles.map(_.tile)))
  }
}
