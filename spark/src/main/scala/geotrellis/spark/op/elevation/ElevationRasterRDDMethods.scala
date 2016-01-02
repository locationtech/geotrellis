package geotrellis.spark.op.elevation

import geotrellis.spark._
import geotrellis.spark.op.focal._
import geotrellis.raster.op.elevation._
import geotrellis.raster.op.focal._

trait ElevationRasterRDDMethods[K] extends FocalOperation[K] {

  def aspect() = {
    val n = Square(1)
    focalWithExtent(n) { (tile, bounds, re) =>
      Aspect(tile, n, bounds, re.cellSize)
    }
  }

  def slope(zFactor: Double = 1.0) = {
    val n = Square(1)
    focalWithExtent(n) { (tile, bounds, re) =>
      Slope(tile, n, bounds, re.cellSize, zFactor)
    }
  }

  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1) = {
    val n = Square(1)
    focalWithExtent(n) { (tile, bounds, re) =>
      Hillshade(tile, n, bounds, re.cellSize, azimuth, altitude, zFactor)
    }
  }

}
