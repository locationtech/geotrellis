package geotrellis.raster.elevation

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._

trait ElevationMethods extends MethodExtensions[Tile] {
  /**
   * Calculates the slope of each cell in a raster.
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   * @see [[Slope]]
   */
  def slope(cs: CellSize, zFactor: Double = 1.0, bounds: Option[GridBounds] = None): Tile =
    Slope(self, Square(1), bounds, cs, zFactor)

  /**
   * Calculates the aspect of each cell in a raster.
   * @param   cs          cellSize of the raster
   * @see [[Aspect]]
   */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None): Tile =
    Aspect(self, Square(1), bounds, cs)


  /**
   * Computes Hillshade (shaded relief) from a raster.
   * @see [[Hillshade]]
   */
  def hillshade(cs: CellSize, azimuth: Double = 315, altitude: Double = 45, zFactor: Double = 1.0, bounds: Option[GridBounds] = None): Tile =
    Hillshade(self, Square(1), bounds, cs, azimuth, altitude, zFactor)
}
