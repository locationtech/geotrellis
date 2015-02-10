package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.raster.stats._
import geotrellis.vector.Extent

import collection._

import spire.syntax.cfor._

/**
  * Takes the most common value in the tile and interpolates all points to that.
  */
class ModeInterpolation(tile: Tile, extent: Extent)
    extends Interpolation(tile, extent) {

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

  override def interpolateValid(x: Double, y: Double): Int =
    mostCommonValue

  override def interpolateDoubleValid(x: Double, y: Double): Double =
    mostCommonValueDouble

}
