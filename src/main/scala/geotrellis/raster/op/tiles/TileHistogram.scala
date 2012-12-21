package geotrellis.raster.op.tiles

import geotrellis._
import geotrellis.statistics._

case class TileHistogram(r: Op[Raster]) extends Reducer1(r)({
  r => FastMapHistogram.fromRaster(r)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})
