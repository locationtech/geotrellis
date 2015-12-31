package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

object CropMethods {
  trait Apply[T] {
    def apply(force: Boolean = false): T
  }
  implicit def applyToCall[T](a: Apply[T]): T = a()
}

trait CropMethods[T <: CellGrid] extends MethodExtensions[T] {

  def crop(gb: GridBounds, force: Boolean): T

  def crop(cols: Int, rows: Int): CropMethods.Apply[T] =
    crop(GridBounds(0, 0, self.cols - 1, self.rows - 1))

  def crop(srcExtent: Extent, extent: Extent): CropMethods.Apply[T] =
    crop(RasterExtent(srcExtent, self).gridBoundsFor(extent))

  def crop(colMin: Int, rowMin: Int, colMax: Int, rowMax: Int): CropMethods.Apply[T] =
    crop(GridBounds(colMin, rowMin, colMax, rowMax))

  def crop(gb: GridBounds): CropMethods.Apply[T] =
    new CropMethods.Apply[T] {
      def apply(force: Boolean = false): T =
        CropMethods.this.crop(gb, force)
    }
}
