package geotrellis.raster.mapalgebra.focal.hillshade

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.util.MethodExtensions


trait HillshadeMethods extends MethodExtensions[Tile] {
  /**
    * Computes Hillshade (shaded relief) from a raster.
    */
  def hillshade(cs: CellSize, azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1.0, bounds: Option[GridBounds] = None): Tile =
    Hillshade(self, Square(1), bounds, cs, azimuth, altitude, zFactor)
}
