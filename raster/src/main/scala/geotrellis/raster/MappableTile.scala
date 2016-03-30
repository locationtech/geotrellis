package geotrellis.raster

import geotrellis.macros._

/**
  *  The [[MappableTile]] trait.
  */
trait MappableTile[T <: MappableTile[T]] extends MacroMappableTile[T] {

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns an integer.
    */
  def map(f: (Int, Int, Int) => Int): T =
    macro TileMacros.intMap_impl[T]

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns a double.
    */
  def mapDouble(f: (Int, Int, Double) => Double): T =
    macro TileMacros.doubleMap_impl[T]
}
