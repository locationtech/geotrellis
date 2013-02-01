package geotrellis.raster.op.tiles

import geotrellis._
import scala.math.max

/**
 * Performs a local Max operation on a TiledRaster.
 *
 * @note               TileMax does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class TileMax(r: Op[Raster]) extends Reducer1(r)({
  r => r.findMinMax._2
})({
  zs => zs.reduceLeft((x, y) => max(x, y))
})
