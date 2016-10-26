package geotrellis.spark.mapalgebra.focal.hillshade

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal.hillshade._
import geotrellis.spark.mapalgebra.focal._

trait HillshadeTileLayerCollectionMethods[K] extends CollectionFocalOperation[K] {

  /** Calculates the hillshade of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Hillshade]]
   */
  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
