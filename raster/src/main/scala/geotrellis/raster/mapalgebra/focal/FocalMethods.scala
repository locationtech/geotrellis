package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait FocalMethods extends MethodExtensions[Tile] {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Min(self, n, bounds, target)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Max(self, n, bounds, target)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Mode(self, n, bounds, target)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Median(self, n, bounds, target)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Mean(self, n, bounds, target)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Sum(self, n, bounds, target)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    StandardDeviation(self, n, bounds, target)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds] = None): Tile = {
    Conway(self, Square(1), bounds)
  }

  /** Computes the convolution of the raster for the given kernl */
  def convolve(kernel: Kernel, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    Convolve(self, kernel, bounds, target)
  }

  /**
    * Calculates spatial autocorrelation of cells based on the
    * similarity to neighboring values.
    */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile = {
    TileMoransICalculation(self, n, bounds, target)
  }

  /**
    * Calculates global spatial autocorrelation of a raster based on
    * the similarity to neighboring values.
    */
  def scalarMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): Double = {
    ScalarMoransICalculation(self, n, bounds)
  }

  /**
    * Calculates the slope of each cell in a raster.
    *
    * @param   cs         cellSize of the raster
    * @param   zFactor    Number of map units to one elevation unit.
    */
  def slope(cs: CellSize, zFactor: Double = 1.0, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    Slope(self, Square(1), bounds, cs, zFactor, target)

  /**
    * Calculates the aspect of each cell in a raster.
    *
    * @param   cs          cellSize of the raster
    */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    Aspect(self, Square(1), bounds, cs, target)
}
