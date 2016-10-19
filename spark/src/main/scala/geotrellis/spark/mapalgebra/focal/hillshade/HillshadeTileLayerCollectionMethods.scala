package geotrellis.spark.mapalgebra.focal.hillshade

import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal.hillshade._
import geotrellis.spark.mapalgebra.focal._

trait HillshadeTileLayerCollectionMethods[K] extends CollectionFocalOperation[K] {
  def hillshade(azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1, target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor, target)
    }
  }
}
