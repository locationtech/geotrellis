package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.summary._
import geotrellis.macros._

import spire.syntax.cfor._

import java.util.Locale

import math.BigDecimal

trait MultibandTile extends CellGrid with MacroCombinableMultibandTile[Tile] with MacroCombineFunctions[Tile, MultibandTile] {
  def bandCount: Int

  def band(bandIndex: Int): Tile

  def convert(newCellType: CellType): MultibandTile

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new integer-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to map over the bands.
    */
  def map(subset: Seq[Int])(f: (Int, Int) => Int): MultibandTile

  /**
    * Map over a subset of the bands of a multiband tile to create a
    * new double-valued multiband tile.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to map over the bands.
    */
  def mapDouble(subset: Seq[Int])(f: (Int, Double) => Double): MultibandTile

  /** Map each band's int value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def map(f: (Int, Int) => Int): MultibandTile

  /** Map each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def mapDouble(f: (Int, Double) => Double): MultibandTile

  /** Map a single band's int value.
    * @param    bandIndex  Band index to map over.
    * @param    f          Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def map(b0: Int)(f: Int => Int): MultibandTile

  /** Map each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the mapped value for that cell value.
    */
  def mapDouble(b0: Int)(f: Double => Double): MultibandTile

  /** Iterate over each band's int value.
    * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreach(f: (Int, Int) => Unit): Unit

  /** Iterate over each band's double value.
    * @param       f       Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreachDouble(f: (Int, Double) => Unit): Unit

  /** Iterate over a single band's int value.
    * @param    bandIndex  Band index to foreach over.
    * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreach(b0: Int)(f: Int => Unit): Unit

  /** Iterate over a single band's double value.
    * @param    bandIndex  Band index to foreach over.
    * @param    f          Function that takes in a band number and a value, and returns the foreachped value for that cell value.
    */
  def foreachDouble(b0: Int)(f: Double => Unit): Unit

  /**
    * Combine a subset of the bands of a tile into a new
    * integer-valued multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    */
  def combine(subset: Seq[Int])(f: Seq[Int] => Int): Tile

  /**
    * Combine a subset of the bands of a tile into a new double-valued
    * multiband tile using the function f.
    *
    * @param    subset   A sequence containing the subset of bands that are of interest.
    * @param    f        A function to combine the bands.
    */
  def combineDouble(subset: Seq[Int])(f: Seq[Double] => Double): Tile

  /** Combine each int band value for each cell.
    * This method will be inherently slower than calling a method with explicitly stated bands,
    * so if you have as many or fewer bands to combine than an explicit method call, use that.
    */
  def combine(f: Array[Int] => Int): Tile

  /** Combine two int band value for each cell.
    */
  def combine(b0: Int, b1: Int)(f: (Int, Int) => Int): Tile

  /** Combine each double band value for each cell.
    * This method will be inherently slower than calling a method with explicitly stated bands,
    * so if you have as many or fewer bands to combine than an explicit method call, use that.
    */
  def combineDouble(f: Array[Double] => Double): Tile

  /** Combine two double band value for each cell.
    */
  def combineDouble(b0: Int, b1: Int)(f: (Double, Double) => Double): Tile

}
