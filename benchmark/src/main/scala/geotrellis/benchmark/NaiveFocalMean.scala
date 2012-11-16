package geotrellis.benchmark

import scala.math.{min, max}

import geotrellis._
import geotrellis.raster.op.focal.Square
import geotrellis.raster.op._
import geotrellis.process._
import geotrellis.raster._

case class NaiveFocalMean(r:Op[Raster], kernel:Square) extends Op1(r)({
  r => new CalcNaiveFocalMean(r, kernel).calc
})

/**
 * This class encompasses an attempt to create a fast sequential focal mean
 * operation, using whatever tricks we can to eke out some speed.
 *
 * We require the raster to contain array data, which we will look up via apply 
 * instead of get. The kernel is a square, and the length of a side is twice
 * the radius plus one. Thus, radius=1 means a 3x3 square kernel (9 cells), and
 * radius=3 means a 7x7 square kernel (49 cells).
 */
final class CalcNaiveFocalMean(r:Raster, kernel:Square) {
  // get an array-like interface to the data
  final val data = r.data.asArray.getOrElse(sys.error("requires array"))

  // raster data used to store the results
  final val out = data.alloc(r.cols, r.rows)

  final def calc = {
    val rows = r.rows
    val cols = r.cols

    val n = kernel.n

    var y = 0
    while (y < rows) {
      val yy1 = max(0, y - n)
      val yy2 = min(rows, y + n)

      var x = 0
      while (x < cols) {
        var sum = 0
        var count = 0

        val xx1 = max(0, x - n)
        val xx2 = min(cols, x + n)

        var yy = yy1
        while (yy < yy2) {
          var xx = 0
          while (xx < xx2) {
            val z = r.get(xx, yy)
            if (z != NODATA) {
              sum += z
              count += 1
            }
            xx += 1
          }
          yy += 1
        }
        if (count > 0) out.set(x, y, sum / count)
        x += 1
      }
      y += 1
    }

    Result(Raster(out, r.rasterExtent))
  }
}
