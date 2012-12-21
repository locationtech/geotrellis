package geotrellis.raster.op.tiles

import geotrellis._
import scala.math.max

case class TileMax(r: Op[Raster]) extends Reducer1(r)({
  r => r.findMinMax._2
})({
  zs => zs.reduceLeft((x, y) => max(x, y))
})
