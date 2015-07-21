package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._
import geotrellis.vector.Extent

import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and resamples all points to that.
  */
class ModeResample(tile: Tile, extent: Extent)
    extends Resample(tile, extent) {

  private lazy val mostCommonValue = FastMapHistogram.fromTile(tile).getMode

  private lazy val mostCommonValueDouble = {
    val map = new java.util.HashMap[Double, Int](tile.size + 10, 1f)
    var max = Double.NaN
    var maxAccum = 0

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val c = tile.getDouble(col, row)
        if (!c.isNaN) {
          var accum = 1
          if (!map.containsKey(c)) {
            map.put(c, 1);
          } else {
            accum = map.get(c);
            map.put(c, accum + 1);
          }

          if (accum > maxAccum) {
            max = c
            maxAccum = accum
          }
        }
      }
    }

    max
  }

  override def resampleValid(x: Double, y: Double): Int =
    mostCommonValue

  override def resampleDoubleValid(x: Double, y: Double): Double =
    mostCommonValueDouble

}
