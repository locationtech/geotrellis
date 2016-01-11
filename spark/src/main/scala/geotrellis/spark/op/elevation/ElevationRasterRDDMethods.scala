package geotrellis.spark.op.elevation

import geotrellis.spark._
import geotrellis.spark.op.focal._
import geotrellis.raster.op.elevation._
import geotrellis.raster.op.focal._

trait ElevationRasterRDDMethods[K] extends FocalOperation[K] {

  def aspect() = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Aspect(tile, n, bounds, cellSize)
    }
  }

  def slope(zFactor: Double = 1.0) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Slope(tile, n, bounds, cellSize, zFactor)
    }
  }

  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor)
    }
  }

}
