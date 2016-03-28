package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell
import geotrellis.util.MethodExtensions


trait FocalMethods extends MethodExtensions[Tile] {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Min(self, n, target, bounds)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Max(self, n, target, bounds)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Mode(self, n, target, bounds)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Median(self, n, target, bounds)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Mean(self, n, target, bounds)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    Sum(self, n, target, bounds)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    StandardDeviation(self, n, target, bounds)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds] = None): Tile = {
    Conway(self, Square(1), bounds)
  }

  /** Computes the convolution of the raster for the given kernl */
  def convolve(kernel: Kernel): Tile = {
    Convolve(self, kernel)
  }

  /** Calculates spatial autocorrelation of cells based on the similarity to neighboring values.
    *
    * @see [[TileMoransICalculation]]
   */
  def tileMoransI(n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile = {
    TileMoransICalculation(self, n, target, bounds)
  }

  /** Calculates global spatial autocorrelation of a raster based on the similarity to neighboring values.
    *
    * @see [[ScalarMoransICalculation]]
   */
  def scalarMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): Double = {
    ScalarMoransICalculation(self, n, bounds)
  }

  /**
    * Calculates the slope of each cell in a raster.
    *
    * @param   cs         cellSize of the raster
    * @param   zFactor    Number of map units to one elevation unit.
    * @see [[Slope]]
    */
  def slope(cs: CellSize, zFactor: Double = 1.0, bounds: Option[GridBounds] = None): Tile =
    Slope(self, Square(1), bounds, cs, zFactor)

  /**
    * Calculates the aspect of each cell in a raster.
    *
    * @param   cs          cellSize of the raster
    * @see [[Aspect]]
    */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None): Tile =
    Aspect(self, Square(1), bounds, cs)
}
