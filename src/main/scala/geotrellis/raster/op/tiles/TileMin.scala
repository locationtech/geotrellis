package geotrellis.raster.op.tiles

import geotrellis._
import scala.math.{ min, max }

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._
import geotrellis.raster._

/**
 * Performs a local Min operation on a TiledRaster.
 *
 * @note               TileMin does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class TileMin(r: Op[Raster]) extends Reducer1(r)({
  r =>
    {
      var zmin = Int.MaxValue
      r.foreach {
        z => if (z != NODATA) zmin = min(z, zmin)
      }
      zmin
    }
})({
  zs => zs.reduceLeft((x, y) => min(x, y))
})

