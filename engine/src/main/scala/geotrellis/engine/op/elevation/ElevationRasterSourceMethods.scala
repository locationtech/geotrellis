package geotrellis.engine.op.elevation

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.engine.op.focal._
import geotrellis.raster.op.elevation._
import geotrellis.raster.op.focal._

trait ElevationRasterSourceMethods extends RasterSourceMethods with FocalOperation {

  def aspect() =
    focalWithExtent(Square(1)){ (tile, hood, bounds,re) =>
      Aspect(tile, hood, bounds, re.cellSize)
    }

  def slope: RasterSource = slope()

  def slope(zFactor: Double = 1.0): RasterSource =
    focalWithExtent(Square(1)){ (tile, hood, bounds, re) =>
      Slope(tile, hood, bounds, re.cellSize, zFactor)
    }

  def hillshade: RasterSource = hillshade()

  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1): RasterSource =
    focalWithExtent(Square(1)){ (tile, hood, bounds, re) =>
      Hillshade(tile, hood, bounds, re.cellSize, azimuth, altitude, zFactor)
    }

}
