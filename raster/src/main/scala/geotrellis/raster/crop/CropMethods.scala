package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait CropMethods[T] extends MethodExtensions[T] {

  def crop(gb: GridBounds, force: Boolean): T

  def crop(gb: GridBounds): T =
    crop(gb, false)


  def crop(cols: Int, rows: Int, force: Boolean): T =
    crop(GridBounds(0, 0, cols - 1, rows - 1), force)

  def crop(cols: Int, rows: Int): T =
    crop(cols, rows, false)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int, force: Boolean): T =
    crop(GridBounds(colMin, rowMin, colMax, rowMax), force)

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): T =
    crop(colMin, rowMin, colMax, rowMax, false)
}

trait CellGridCropMethods[T <: CellGrid] extends CropMethods[T] {

  def crop(srcExtent: Extent, extent: Extent, force: Boolean): T =
    crop(RasterExtent(srcExtent, self).gridBoundsFor(extent), force)

  def crop(srcExtent: Extent, extent: Extent): T =
    crop(srcExtent, extent, false)

}
