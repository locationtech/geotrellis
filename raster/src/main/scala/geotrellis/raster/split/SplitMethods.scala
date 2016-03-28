package geotrellis.raster.split

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import Split.Options

trait SplitMethods[T] extends MethodExtensions[T] {
  /**
    * Splits this into an array of elements based on a TileLayout.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        tileLayout     TileLayout defining the tiles to be generated
    *
    * @return                      An array of T
    */
  def split(tileLayout: TileLayout): Array[T] =
    split(tileLayout, Options.DEFAULT)

  /**
    * Splits this into an array of elements based on a TileLayout.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        tileLayout     TileLayout defining the tiles to be generated
    * @param        options        Options that control the split
    *
    * @return                      An array of T
    */
  def split(tileLayout: TileLayout, options: Options): Array[T]
}
