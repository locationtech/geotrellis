package geotrellis.raster.op.focal

import geotrellis.raster._

trait FocalMethods extends TileMethods {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    Min(tile, n).execute(analysisArea)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    Max(tile, n).execute(analysisArea)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    ModeCalculation(tile, n).execute(analysisArea)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    MedianCalculation(tile, n).execute(analysisArea)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    MeanCalculation(tile, n).execute(analysisArea)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    Sum(tile, n).execute(analysisArea)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    StandardDeviation(tile, n).execute(analysisArea)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    Conway(tile, n).execute(analysisArea)
  }

  /**
   * Calculates the slope of each cell in a raster.
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   * @see [[Slope]]
   */
  def slope(cs: CellSize, zFactor: Double, analysisArea: Option[GridBounds] = None): Tile = {
    Slope(tile, Square(1), cs, zFactor).execute(analysisArea)
  }

  /**
   * Calculates the aspect of each cell in a raster.
   * @param   cs          cellSize of the raster
   * @see [[Aspect]]
   */
  def aspect(cs: CellSize, analysisArea: Option[GridBounds] = None): Tile = {
    Aspect(tile, Square(1), cs).execute(analysisArea)
  }

  /**
   * Computes Hillshade (shaded relief) from a raster.
   * @see [[Hillshade]]
   */
  def hillshade(cs: CellSize, azimuth: Double, altitude: Double, zFactor: Double, analysisArea: Option[GridBounds] = None) = {
    Hillshade(tile, Square(1), cs, azimuth, altitude, zFactor).execute(analysisArea)
  }

  /** Calculates spatial autocorrelation of cells based on the similarity to neighboring values.
   * @see [[TileMoransICalculation]]
   */
  def tileMoransI(n: Neighborhood, analysisArea: Option[GridBounds] = None): Tile = {
    TileMoransICalculation(tile, n).execute(analysisArea)
  }

  /** Calculates global spatial autocorrelation of a raster based on the similarity to neighboring values.
   * @see [[ScalarMoransICalculation]]
   */
  def scalarMoransI(n: Neighborhood, analysisArea: Option[GridBounds] = None): Double = {
    ScalarMoransICalculation(tile, n).execute(analysisArea)
  }
}
