package geotrellis.layers.mapalgebra.focal.hillshade

import geotrellis.layers.mapalgebra.focal.CollectionFocalOperation
import geotrellis.raster.DoubleConstantNoDataCellType
import geotrellis.raster.mapalgebra.focal.hillshade.Hillshade
import geotrellis.raster.mapalgebra.focal.{Square, TargetCell}

trait HillshadeTileLayerCollectionMethods[K] extends CollectionFocalOperation[K] {
  /** Calculates the hillshade of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Hillshade]]
   */
  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1, target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
