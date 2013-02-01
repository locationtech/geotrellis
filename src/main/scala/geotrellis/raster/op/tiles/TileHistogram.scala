package geotrellis.raster.op.tiles

import geotrellis._
import geotrellis.statistics._

/**
 * Creates a histogram for a TiledRaster
 *
 * @note               TileHistogram does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class TileHistogram(r: Op[Raster]) extends Reducer1(r)({
  r => FastMapHistogram.fromRaster(r)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})
