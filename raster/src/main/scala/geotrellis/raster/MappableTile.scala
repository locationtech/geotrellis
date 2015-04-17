package geotrellis.raster

import geotrellis.macros._

trait MappableTile[T <: MappableTile[T]] extends MacroMappableTile[T] {
  def foreach(f: (Int, Int, Int) => Int): T =
    macro TileMacros.intMap_impl[T]

  def foreachDouble(f: (Int, Int, Double) => Double): T =
    macro TileMacros.doubleMap_impl[T]
}
