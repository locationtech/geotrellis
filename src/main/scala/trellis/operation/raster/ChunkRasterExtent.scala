package trellis.operation

import trellis.RasterExtent
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
case class ChunkRasterExtent(g:Op[RasterExtent], nx:Int, ny:Int)
extends CachedOp[Seq[Op[RasterExtent]]] with SimpleOp[Seq[Op[RasterExtent]]] {

  def _value(context:Context) = {
    val geo = context.run(g)
    val a = Array.ofDim[Op[RasterExtent]](ny * nx)

    // calculate the break points along the X and Y axes
    var xlimits = (0 to nx).map(i => geo.extent.xmin + ((i * geo.width) / nx))
    var ylimits = (0 to ny).map(i => geo.extent.ymin + ((i * geo.height) / ny))

    var y = 0
    while (y < ny) {
      var x = 0
      while (x < nx) {
        val ymin = ylimits(y)
        val ymax = ylimits(y + 1)
        val xmin = xlimits(x)
        val xmax = xlimits(x + 1)
        a(y * nx + x) = BuildRasterExtent(xmin, ymin, xmax, ymax,
                                          ((xmax - xmin) / geo.cellwidth).toInt,
                                          ((ymax - ymin) / geo.cellheight).toInt)
        x += 1
      }
      y += 1
    }
    a
  }
}
