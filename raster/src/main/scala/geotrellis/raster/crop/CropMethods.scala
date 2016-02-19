package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait CropMethods[T] extends MethodExtensions[T] {
  import Crop.Options

  def crop(gb: GridBounds, options: Options): T

  def crop(gb: GridBounds): T =
    crop(gb, Options.DEFAULT)


  def crop(cols: Int, rows: Int, options: Options): T =
    crop(GridBounds(0, 0, cols - 1, rows - 1), options)

  def crop(cols: Int, rows: Int): T =
    crop(cols, rows, Options.DEFAULT)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int, options: Options): T =
    crop(GridBounds(colMin, rowMin, colMax, rowMax), options)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): T =
    crop(colMin, rowMin, colMax, rowMax, Options.DEFAULT)
}
