package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * A trait housing extension methods for cropping.
  */
trait CropMethods[T] extends MethodExtensions[T] {
  import Crop.Options

  /**
    * Given a [[GridBounds]] and some cropping options, crop.
    */
  def crop(gb: GridBounds, options: Options): T

  /**
    * Given a [[GridBounds]], crop.
    */
  def crop(gb: GridBounds): T =
    crop(gb, Options.DEFAULT)

  /**
    * Given a number of columns and rows for the desired output and
    * some cropping options, crop.
    */
  def crop(cols: Int, rows: Int, options: Options): T =
    crop(GridBounds(0, 0, cols - 1, rows - 1), options)

  /**
    * Given a number of columns and rows for the desired output, crop.
    */
  def crop(cols: Int, rows: Int): T =
    crop(cols, rows, Options.DEFAULT)

  /**
    * Given the starting and stopping columns and rows and some
    * cropping options, crop.
    */
  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int, options: Options): T =
    crop(GridBounds(colMin, rowMin, colMax, rowMax), options)

  /**
    * Given the starting and stopping columns and rows, crop.
    */
  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): T =
    crop(colMin, rowMin, colMax, rowMax, Options.DEFAULT)
}
