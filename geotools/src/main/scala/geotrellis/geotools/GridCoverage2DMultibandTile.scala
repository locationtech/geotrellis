package geotrellis.geotools

import geotrellis.raster._

import org.geotools.coverage.grid._
import org.geotools.gce.geotiff._
import org.geotools.coverage.grid.io._


// XXX ScalaDocs
object GridCoverage2DMultibandTile {
  def apply(gridCoverage: GridCoverage2D) =
    new GridCoverage2DMultibandTile(gridCoverage)
}

class GridCoverage2DMultibandTile(gridCoverage: GridCoverage2D)
    extends MultibandTile with MacroMultibandCombiners {

  val renderedImage = gridCoverage.getRenderedImage
  val buffer = renderedImage.getData.getDataBuffer
  val sampleModel = renderedImage.getSampleModel

  private val _array = Array.ofDim[Int](bandCount)
  private val _arrayDouble = Array.ofDim[Double](bandCount)
  private def getFiber(col: Int, row: Int) = sampleModel.getPixel(col, row, _array, buffer)
  private def getFiberDouble(col: Int, row: Int) = sampleModel.getPixel(col, row, _arrayDouble, buffer)

  val bandCount: Int = renderedImage.getSampleModel.getNumBands

  val cellType: CellType = GridCoverage2DToRaster.cellType(gridCoverage)

  val rows: Int = renderedImage.getHeight

  val cols: Int = renderedImage.getWidth

  def band(bandIndex: Int) =
    GridCoverage2DTile(gridCoverage, bandIndex)

  def bands =
    (0 until bandCount).map({ b => band(b) }).toVector

  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile =
    combine(List(b0, b1))({ xs: Seq[Int] => f(xs.head, xs.tail.head) })

  def combine(f: Array[Int] => Int): Tile =
    combine(0 until bandCount)({ xs: Seq[Int] => f(xs.toArray) })

  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile = {
    val result = ArrayTile.alloc(cellType, cols, rows)

    var row = 0; while (row < rows) {
      var col = 0; while (col < cols) {
        val array = getFiber(col, row)
        val data = subset.map({ b => array(b) })
        result.set(col, row, f(data))
        col += 1
      }
      row += 1
    }

    result
  }

  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile =
    combineDouble(List(b0, b1))({ xs: Seq[Double] => f(xs.head, xs.tail.head) })

  def combineDouble(f: Array[Double] => Double): Tile =
    combineDouble(0 until bandCount)({ xs: Seq[Double] => f(xs.toArray) })

  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile = {
    val result = ArrayTile.alloc(cellType.union(DoubleCellType), cols, rows)

    var row = 0; while (row < rows) {
      var col = 0; while (col < cols) {
        val array = getFiberDouble(col, row)
        val data = subset.map({ b => array(b) })
        result.setDouble(col, row, f(data))
        col += 1
      }
      row += 1
    }

    result
  }

  def foreach(b0: Int)(f: Int => Unit): Unit =
    band(b0).foreach(f)

  def foreach(f: (Int, Int) => Unit): Unit = {
    bands.zipWithIndex
      .foreach({ case (tile, i) =>
        tile.foreach(f(i,_))
      })
  }

  def foreachDouble(b0: Int)(f: Double => Unit): Unit =
    band(b0).foreachDouble(f)

  def foreachDouble(f: (Int, Double) => Unit): Unit = {
    bands.zipWithIndex
      .foreach({ case (tile, i) =>
        tile.foreachDouble(f(i,_))
      })
  }

  def map(b0: Int)(f: Int => Int): MultibandTile =
    map(List(b0))({ (_,z) => f(z) })

  def map(f: (Int, Int) => Int): MultibandTile =
    map(0 until bandCount)(f)

  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile = {
    val set = subset.toSet
    val newBands = bands
      .zipWithIndex
      .map({ case (tile, i) =>
        if (set.contains(i))
          tile.map(f(i,_))
        else if (cellType.isFloatingPoint)
          tile.map({ z => z })
        else
          tile
      })
      .toArray

    ArrayMultibandTile(newBands)
  }

  def mapDouble(b0: Int)(f: Double => Double): MultibandTile =
    mapDouble(List(b0))({ (_,z) => f(z) })

  def mapDouble(f: (Int, Double) => Double): MultibandTile =
    mapDouble(0 until bandCount)(f)

  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile = {
    val set = subset.toSet
    val newBands = bands
      .zipWithIndex
      .map({ case (tile, i) =>
        if (set.contains(i))
          tile.mapDouble(f(i,_))x
        else if (cellType.isFloatingPoint)
          tile
        else
          tile.mapDouble({ z => z })
      })
      .toArray

    ArrayMultibandTile(newBands)
  }

  def convert(newCellType: geotrellis.raster.CellType): MultibandTile =
    ArrayMultibandTile(bands.map(_.convert(newCellType)).toArray)

  def subsetBands(bandSequence: Seq[Int]): MultibandTile =
    ArrayMultibandTile(bandSequence.map({ b => GridCoverage2DTile(gridCoverage, b) }).toArray)
}
