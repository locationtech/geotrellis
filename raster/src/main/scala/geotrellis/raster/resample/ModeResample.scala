package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._
import geotrellis.vector.Extent

import scala.collection.mutable
import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and resamples all points to that.
  */
class ModeResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateIntMode(indices: Seq[(Int, Int)]): Double = {
    indices.foldLeft(mutable.Map[Int, Int]()) { (hash, coords) =>
      val v = tile.get(coords._1, coords._2)
      hash(v) = hash.getOrElseUpdate(v, 0) + 1
      hash
    }
  }

  override def resampleValid(x: Double, y: Double): Int =
    mostCommonValue

  override def resampleDoubleValid(x: Double, y: Double): Double =
    mostCommonValueDouble

}
