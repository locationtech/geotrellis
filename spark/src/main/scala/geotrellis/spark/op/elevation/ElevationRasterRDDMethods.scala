package geotrellis.spark.op.elevation

import geotrellis.spark._
import geotrellis.spark.op.focal._
import geotrellis.raster.op.elevation._
import geotrellis.raster.op.focal._

trait ElevationRasterRDDMethods[K] extends RasterRDDMethods[K]
    with FocalOperation[K] {

  def aspect() =
    focalWithExtent(Square(1)) { (tile, hood, bounds, re) =>
      Aspect(tile, hood, bounds, re.cellSize)
    }

  def slope(zFactor: Double = 1.0) =
    focalWithExtent(Square(1)) { (tile, hood, bounds, re) =>
      Slope(tile, hood, bounds, re.cellSize, zFactor)
    }

  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1) =
    focalWithExtent(Square(1)) { (tile, hood, bounds, re) =>
      Hillshade(tile, hood, bounds, re.cellSize, azimuth, altitude, zFactor)
    }

}
