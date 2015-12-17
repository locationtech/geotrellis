package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._
import geotrellis.vector.Extent

import scala.collection.mutable
import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in a region and resamples to that.
  *
  * As with other aggregate resampling methods, this is most useful when
  * decreasing resolution or downsampling.
  *
  */
class ModeResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateIntMode(indices: Seq[(Int, Int)]): Int = {
    indices.foldLeft(mutable.Map[Int, Int]()) { (hash, coords) =>
      val v = tile.get(coords._1, coords._2)
      hash(v) = hash.getOrElseUpdate(v, 0) + 1
      hash
    }.toSeq.maxBy { case (key, value) => value }._1
  }

  private def calculateDoubleMode(indices: Seq[(Int, Int)]): Double = {
    indices.foldLeft(mutable.Map[Double, Int]()) { (hash, coords) =>
      val v = tile.getDouble(coords._1, coords._2)
      hash(v) = hash.getOrElseUpdate(v, 0) + 1
      hash
    }.toSeq.maxBy { case (key, value) => value }._1
  }

  override def resampleValid(x: Double, y: Double): Int =
    calculateIntMode(contributions(x, y))

  override def resampleDoubleValid(x: Double, y: Double): Double =
    calculateDoubleMode(contributions(x, y))

}
