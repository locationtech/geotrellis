package geotrellis.spark.streaming.mapalgebra.focal.hillshade

import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal.hillshade._
import geotrellis.spark.streaming.mapalgebra.focal._

trait HillshadeTileLayerDStreamMethods[K] extends DStreamFocalOperation[K] {
  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor)
    }
  }
}
