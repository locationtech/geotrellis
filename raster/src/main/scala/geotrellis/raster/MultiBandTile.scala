package geotrellis.raster

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent
import geotrellis.raster.op.stats._

import spire.syntax.cfor._

import java.util.Locale

import math.BigDecimal

trait MultiBandTile {
  def bandCount: Int
  def cellType: CellType
  def cols: Int
  def rows: Int

  def dimensions: (Int, Int) = (cols, rows)
  def size = cols * rows

  def band(bandIndex: Int): Tile

  // /** Map each band's int value.
  //   * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
  //   */
  // def map(f: (Int, Int) => Int): MultiBandTile

  // /** Map each band's double value.
  //   * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
  //   */
  // def mapDouble(f: (Int, Double) => Double): MultiBandTile

  // /** Map a single band's int value.
  //   * @param    bandIndex  Band index to map over.
  //   * @param    f          Function that takes in a band number and a value, and returns the mapped value for that cell value.
  //   */
  // def map(b0: Int)(f: Int => Int): MultiBandTile

  // /** Map each band's double value.
  //   * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
  //   */
  // def mapDouble(b0: Int)(f: Double => Double): MultiBandTile

  // /** Iterate over each band's int value.
  //   * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
  //   */
  // def foreach(f: (Int, Int) => Unit): Unit

  // /** Iterate over each band's double value.
  //   * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
  //   */
  // def foreachDouble(f: (Int, Double) => Unit): Unit

  // /** Iterate over a single band's int value.
  //   * @param    bandIndex  Band index to foreach over.
  //   * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
  //   */
  // def foreach(b0: Int)(f: Int => Unit): Unit

  // /** Iterate over a single band's double value.
  //   * @param    bandIndex  Band index to foreach over.
  //   * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
  //   */
  // def foreachDouble(b0: Int)(f: Double => Unit): Unit

  // /** Combine each int band value for each cell.
  //   * This method will be inherently slower than calling a method with explicitly stated bands,
  //   * so if you have as many or fewer bands to combine than an explicit method call, use that.
  //   */
  // def combine(f: Array[Int] => Int)

  // /** Combine two int band value for each cell.
  //   */
  // def combine(b0: Int, b1: Int)(f: (Int, Int) => Int)

  // /** Combine three int band value for each cell.
  //   * Note: this method uses macros to side step the inefficiency of Function3 not being specialized.
  //   */
  // def combine(b0: Int, b1: Int, b2: Int)(f: (Int, Int, Int) => Int)

  // /** Combine four int band value for each cell.
  //   * Note: this method uses macros to side step the inefficiency of Function4 not being specialized.
  //   */
  // def combine(b0: Int, b1: Int, b2: Int, b3: Int)(f: (Int, Int, Int, Int) => Int)

  // /** Combine each double band value for each cell.
  //   * This method will be inherently slower than calling a method with explicitly stated bands,
  //   * so if you have as many or fewer bands to combine than an explicit method call, use that.
  //   */
  // def combineDouble(f: Array[Double] => Double)

  // /** Combine two double band value for each cell.
  //   */
  // def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double)

  // /** Combine three double band value for each cell.
  //   * Note: this method uses macros to side step the inefficiency of Function3 not being specialized.
  //   */
  // def combineDouble(b0: Int, b1: Int, b2: Int)(f: (Double, Double, Double) => Double)

  // /** Combine four double band value for each cell.
  //   * Note: this method uses macros to side step the inefficiency of Function4 not being specialized.
  //   */
  // def combineDouble(b0: Int, b1: Int, b2: Int, b3: Int)(f: (Double, Double, Double, Double) => Double)

  // def resample(source: Extent, target: RasterExtent): MultiBandTile =
  //   resample(source, target, InterpolationMethod.DEFAULT)

  // def resample(source: Extent, target: RasterExtent, method: InterpolationMethod): MultiBandTile

  // def resample(source: Extent, target: Extent): MultiBandTile =
  //   resample(source, target, InterpolationMethod.DEFAULT)

  // def resample(source: Extent, target: Extent, method: InterpolationMethod): MultiBandTile =
  //   resample(source, RasterExtent(source, cols, rows).createAligned(target), method)

  // def resample(source: Extent, targetCols: Int, targetRows: Int): MultiBandTile =
  //   resample(source, targetCols, targetRows, InterpolationMethod.DEFAULT)

  // def resample(source: Extent, targetCols: Int, targetRows: Int, method: InterpolationMethod): MultiBandTile =
  //   resample(source, RasterExtent(source, targetCols, targetRows), method)

  // /** Only changes the resolution */
  // def resample(targetCols: Int, targetRows: Int): MultiBandTile =
  //   resample(Extent(0.0, 0.0, 1.0, 1.0), targetCols, targetRows)
}
