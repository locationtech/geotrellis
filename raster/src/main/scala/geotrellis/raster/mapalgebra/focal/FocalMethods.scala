package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait FocalMethods extends MethodExtensions[Tile] {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Min(self, n, bounds)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Max(self, n, bounds)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Mode(self, n, bounds)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Median(self, n, bounds)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Mean(self, n, bounds)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    Sum(self, n, bounds)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    StandardDeviation(self, n, bounds)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds] = None): Tile = {
    Conway(self, Square(1), bounds)
  }

  /** Computes the convolution of the raster for the given kernl */
  def convolve(kernel: Kernel): Tile = {
    Convolve(self, kernel)
  }

  /**
    * Calculates spatial autocorrelation of cells based on the
    * similarity to neighboring values.
    */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds] = None): Tile = {
    TileMoransICalculation(self, n, bounds)
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
  def slope(cs: CellSize, zFactor: Double = 1.0, bounds: Option[GridBounds] = None): Tile =
    Slope(self, Square(1), bounds, cs, zFactor)

  /**
    * Calculates the aspect of each cell in a raster.
    *
    * @param   cs          cellSize of the raster
    */
  def aspect(cs: CellSize, bounds: Option[GridBounds] = None): Tile =
    Aspect(self, Square(1), bounds, cs)
}
