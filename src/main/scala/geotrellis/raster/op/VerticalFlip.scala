package geotrellis.raster.op

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Flip the data for a raster along the X-axis.
 *
 * The geographic extent will remain unchanged.
 */
case class VerticalFlip(r:Op[Raster]) extends Op1(r) ({
  r =>
  val cols = r.cols
  val rows = r.cols
  val data = r.data.asArray.getOrElse(sys.error("can't get data array"))
  val data2 = data.alloc(cols, rows)

  var y = 0
  var x = 0
  while (y < rows) {
    x = 0
    val yspan = y * cols
    val yspan2 = (cols - 1 - y) * cols
    while (x < cols) {
      data2(yspan2 + x) = data(yspan + x)
      x += 1
    }
    y += 1
  }
  Result(Raster(data2,r.rasterExtent))
})
