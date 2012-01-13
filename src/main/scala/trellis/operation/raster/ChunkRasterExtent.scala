package trellis.operation

import trellis._
import trellis.process._

// ny=4    nx=4    nx=2 ny=2
// AAAA    ABCD    AABB
// BBBB    ABCD    AABB
// CCCC    ABCD    CCDD
// DDDD    ABCD    CCDD

/**
 * Used to chunk a RasterExtent object (geographical extent + grid information)
 * into many smaller contiguous pieces. The number of columns desired is
 * provided by `nx` and the number of rows by `ny`.
 */
case class ChunkRasterExtent(g:Op[RasterExtent], opnx:Op[Int], opny:Op[Int])
extends SimpleOp[Array[RasterExtent]] {

  def _value(context:Context) = {
    val re = context.run(g)
    val nx = context.run(opnx)
    val ny = context.run(opny)
    val a = Array.ofDim[RasterExtent](ny * nx)

    // calculate the break points along the X and Y axes
    val e = re.extent
    var xlimits = (0 to nx).map(e.xmin + _.toDouble / nx * e.width)
    var ylimits = (0 to ny).map(e.ymin + _.toDouble / ny * e.height)

    var y = 0
    while (y < ny) {
      var x = 0
      while (x < nx) {
        val ymin = ylimits(y)
        val ymax = ylimits(y + 1)
        val xmin = xlimits(x)
        val xmax = xlimits(x + 1)

        val extent = Extent(xmin, ymin, xmax, ymax)
        val cols = ((xmax - xmin) / re.cellwidth).toInt
        val rows = ((ymax - ymin) / re.cellheight).toInt

        val re2 = RasterExtent(extent, re.cellwidth, re.cellheight, cols, rows)
        a(y * nx + x) = re2

        x += 1
      }
      y += 1
    }
    a
  }
}
