package geotrellis.raster.op.focal

import geotrellis.raster._

trait FocalMethods extends TileMethods {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood): Tile = {
    val calc = MinCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood): Tile = {
    val calc = MaxCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood): Tile = {
    val calc = ModeCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood): Tile = {
    val calc = MedianCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood): Tile = {
    val calc = MeanCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood): Tile = {
    val calc = SumCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood): Tile = {
    val calc = StandardDeviationCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Computes the next step of Conway's Game of Life */
  def focalConway(n: Neighborhood): Tile = {
    val calc = ConwayCalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /**
   * Calculates the slope of each cell in a raster.
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   * @see [[SlopeCalculation]]
   */
  def focalSlope(cs: CellSize, zFactor: Double): Tile = {
    val calc = SlopeCalculation(tile, Square(1))
    calc.init(tile, cs, zFactor)
    calc.execute(tile, Square(1))
  }

  /**
   * Calculates the aspect of each cell in a raster.
   * @param   cs          cellSize of the raster
   * @see [[AspectCalculation]]
   */
  def aspect(cs: CellSize): Tile = {
    val calc = AspectCalculation(tile, Square(1))
    calc.init(tile, cs)
    calc.execute(tile, Square(1))
  }

  /**
   * Computes Hillshade (shaded relief) from a raster.
   * @see [[HillshadeCalculation]]
   */
  def hillshade(cs: CellSize, azimuth: Double, altitude: Double, zFactor: Double) = {
    val calc = HillshadeCalculation(tile, Square(1))
    calc.init(tile, cs, azimuth, altitude, zFactor)
    calc.execute(tile, Square(1))
  }

  /** Calculates spatial autocorrelation of cells based on the similarity to neighboring values.
   * @see [[TileMoransICalculation]]
   */
  def tileMoransI(n: Neighborhood): Tile = {
    val calc = TileMoransICalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }

  /** Calculates global spatial autocorrelation of a raster based on the similarity to neighboring values.
   * @see [[ScalarMoransICalculation]]
   */
  def scalarMoransI(n: Neighborhood): Double = {
    val calc = ScalarMoransICalculation(tile, n)
    calc.init(tile)
    calc.execute(tile, n)
  }
}
