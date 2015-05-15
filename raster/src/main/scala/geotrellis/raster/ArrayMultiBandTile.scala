package geotrellis.raster

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent
import geotrellis.raster.op.stats._

import spire.syntax.cfor._

object ArrayMultiBandTile {
  def apply(bands: Tile*): ArrayMultiBandTile =
    apply(bands.toArray)

  def apply(bands: Array[Tile]): ArrayMultiBandTile =
    new ArrayMultiBandTile(bands)
}

class ArrayMultiBandTile(bands: Array[Tile]) extends MultiBandTile {
  val bandCount = bands.size

  assert(bandCount > 0, "Band count must be greater than 0")
  private def validateBand(i: Int) = assert(i < bandCount, s"Band index out of bounds. Band Count: $bandCount Requested Band Index: $i")

  val cellType = bands(0).cellType
  val cols: Int = bands(0).cols
  val rows: Int = bands(0).rows

  // Check all bands for consistency.
  cfor(0)(_ < bandCount, _ + 1) { i =>
    assert(bands(i).cellType == cellType, s"Band $i cell type does not match, ${bands(i).cellType} != $cellType")
    assert(bands(i).cols == cols, s"Band $i cols does not match, ${bands(i).cols} != $cols")
    assert(bands(i).rows == rows, s"Band $i rows does not match, ${bands(i).rows} != $rows")
  }

  def band(bandIndex: Int): Tile = {
    if(bandIndex >= bandCount) { throw new IllegalArgumentException(s"Band $bandIndex does not exist") }
    bands(bandIndex)
  }

  /** Map each band's int value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def map(f: (Int, Int) => Int): MultiBandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      newBands(i) = band(i).map { z => f(i, z) }
    }

    ArrayMultiBandTile(newBands)
  }

  /** Map each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def mapDouble(f: (Int, Double) => Double): MultiBandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      newBands(i) = band(i).mapDouble { z => f(i, z) }
    }

    ArrayMultiBandTile(newBands)
  }

  /** Map a single band's int value.
    * @param    bandIndex  Band index to map over.
    * @param    f          Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def map(b0: Int)(f: Int => Int): MultiBandTile = {
    validateBand(b0)
    val newBands = bands.clone
    newBands(b0) = band(b0) map f

    ArrayMultiBandTile(newBands)
  }

  /** Map each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def mapDouble(b0: Int)(f: Double => Double): MultiBandTile = {
    validateBand(b0)
    val newBands = bands.clone
    newBands(b0) = band(b0) mapDouble f

    ArrayMultiBandTile(newBands)
  }

  /** Iterate over each band's int value.
    * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreach(f: (Int, Int) => Unit): Unit = {
    cfor(0)(_ < bandCount, _ + 1) { i =>
      band(i).foreach { z => f(i, z) }
    }
  }

  /** Iterate over each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreachDouble(f: (Int, Double) => Unit): Unit = {
    cfor(0)(_ < bandCount, _ + 1) { i =>
      band(i).foreachDouble { z => f(i, z) }
    }
  }

  /** Iterate over a single band's int value.
    * @param    bandIndex  Band index to foreach over.
    * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreach(b0: Int)(f: Int => Unit): Unit = {
    validateBand(b0)
    band(b0) foreach f
  }

  /** Iterate over a single band's double value.
    * @param    bandIndex  Band index to foreach over.
    * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreachDouble(b0: Int)(f: Double => Unit): Unit = {
    validateBand(b0)
    band(b0) foreachDouble f
  }

  /** Combine each int band value for each cell.
    * This method will be inherently slower than calling a method with explicitly stated bands,
    * so if you have as many or fewer bands to combine than an explicit method call, use that.
    */
  def combine(f: Array[Int] => Int): Tile = {
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < bandCount, _ + 1) { b =>
          arr(b) = band(b).get(row, col)
        }
        result.set(col, row, f(arr))
      }
    }
    result
  }

  /** Combine two int band value for each cell.
    */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile = {
    val band1 = band(b0)
    val band2 = band(b1)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.set(col, row, f(band1.get(col, row), band2.get(col, row)))
      }
    }
    result
  }

  /** Combine three int band value for each cell.
    */
  def combineIntTileCombiner(combiner: IntTileCombiner3): Tile = {
    val band1 = band(combiner.b0)
    val band2 = band(combiner.b1)
    val band3 = band(combiner.b2)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.set(col, row, combiner(band1.get(col, row), band2.get(col, row), band3.get(col,row)))
      }
    }
    result
  }

  /** Combine four int band value for each cell.
    */
  def combineIntTileCombiner(combiner: IntTileCombiner4): Tile = {
    val band1 = band(combiner.b0)
    val band2 = band(combiner.b1)
    val band3 = band(combiner.b2)
    val band4 = band(combiner.b3)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.set(col, row, combiner(band1.get(col, row), band2.get(col, row), band3.get(col,row), band4.get(col, row)))
      }
    }
    result
  }

  /** Combine each double band value for each cell.
    * This method will be inherently slower than calling a method with explicitly stated bands,
    * so if you have as many or fewer bands to combine than an explicit method call, use that.
    */
  def combineDouble(f: Array[Double] => Double) = {
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Double](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < bandCount, _ + 1) { b =>
          arr(b) = band(b).getDouble(row, col)
        }
        result.setDouble(col, row, f(arr))
      }
    }
    result
  }

  /** Combine two double band value for each cell.
    */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile = {
    val band1 = band(b0)
    val band2 = band(b1)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.setDouble(col, row, f(band1.getDouble(col, row), band2.getDouble(col, row)))
      }
    }
    result
  }

  /** Combine three double band value for each cell.
    */
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner3): Tile = {
    val band1 = band(combiner.b0)
    val band2 = band(combiner.b1)
    val band3 = band(combiner.b2)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.setDouble(col, row, combiner(band1.getDouble(col, row), band2.getDouble(col, row), band3.getDouble(col,row)))
      }
    }
    result
  }

  /** Combine four double band value for each cell.
    */
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner4) = {
    val band1 = band(combiner.b0)
    val band2 = band(combiner.b1)
    val band3 = band(combiner.b2)
    val band4 = band(combiner.b3)
    val result = ArrayTile.empty(cellType, cols, rows)
    val arr = Array.ofDim[Int](bandCount)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        result.setDouble(col, row, combiner(band1.getDouble(col, row), band2.getDouble(col, row), band3.getDouble(col,row), band4.getDouble(col, row)))
      }
    }
    result
  }
}
