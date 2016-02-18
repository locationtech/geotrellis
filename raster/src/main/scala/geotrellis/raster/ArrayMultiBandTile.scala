package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.summary._

import spire.syntax.cfor._

object ArrayMultiBandTile {
  def apply(bands: Tile*): ArrayMultiBandTile =
    apply(bands.toArray)

  def apply(bands: Traversable[Tile]): ArrayMultiBandTile =
    new ArrayMultiBandTile(bands.toArray)

  def apply(bands: Array[Tile]): ArrayMultiBandTile =
    new ArrayMultiBandTile(bands)

  def alloc(t: CellType, bands: Int, cols: Int, rows: Int): ArrayMultiBandTile = {
    ArrayMultiBandTile(for (_ <- 0 until bands) yield ArrayTile.alloc(t, cols, rows))
  }

  def empty(t: CellType, bands: Int, cols: Int, rows: Int): ArrayMultiBandTile = {
    ArrayMultiBandTile(for (_ <- 0 until bands) yield ArrayTile.empty(t, cols, rows))
  }
}

class ArrayMultiBandTile(bands: Array[Tile]) extends MultiBandTile with MacroMultibandCombiners {
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

  def convert(newCellType: CellType): MultiBandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      newBands(i) = band(i).convert(newCellType)
    }

    ArrayMultiBandTile(newBands)
  }

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new integer-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to map over the bands.
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultiBandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    val set = subset.toSet

    subset.foreach({ b =>
      require(0 <= b && b < bandCount, "All elements of subset must be present")
    })

    (0 until bandCount).foreach({ b =>
      if (set.contains(b))
        newBands(b) = band(b).map({ z => f(b, z) })
      else if (cellType.isFloatingPoint)
        newBands(b) = band(b).map({ z => z })
      else
        newBands(b) = band(b)
    })

    ArrayMultiBandTile(newBands)
  }

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new double-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to map over the bands.
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultiBandTile = {
    val newBands = Array.ofDim[Tile](bandCount)
    val set = subset.toSet

    subset.foreach({ b =>
      require(0 <= b && b < bandCount, "All elements of subset must be present")
    })

    (0 until bandCount).foreach({ b =>
      if (set.contains(b))
        newBands(b) = band(b).mapDouble({ z => f(b, z) })
      else if (cellType.isFloatingPoint)
        newBands(b) = band(b)
      else
        newBands(b) = band(b).mapDouble({ z => z })
    })

    ArrayMultiBandTile(newBands)
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

  /**
    * Combine a subset of the bands of a tile into a new
    * integer-valued multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })

    val result = ArrayTile.empty(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val data = subset.map({ b => band(b).get(col, row) })
        result.set(col, row, f(data))
      }
    }
    result
  }

  /**
    * Combine a subset of the bands of a tile into a new double-valued
    * multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile = {
    subset.foreach({ b => require(0 <= b && b < bandCount, "All elements of subset must be present") })

    val result = ArrayTile.empty(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val data = subset.map({ b => band(b).getDouble(col, row) })
        result.setDouble(col, row, f(data))
      }
    }
    result
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
          arr(b) = band(b).get(col, row)
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
          arr(b) = band(b).getDouble(col, row)
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

  def subset(bands: Seq[Int]): ArrayMultiBandTile = {
    val newBands = Array.ofDim[Tile](bands.size)
    var i = 0

    require(bands.size <= this.bandCount)
    bands.foreach({ j =>
      newBands(i) = this.band(j)
      i += 1
    })

    new ArrayMultiBandTile(newBands)
  }

  def subset(bands: Int*)(implicit d: DummyImplicit): ArrayMultiBandTile =
    subset(bands)
}
