package geotrellis.raster

import geotrellis.macros._

trait IterableTile extends MacroIterableTile {
  def foreach(f: (Int, Int, Int) => Unit): Unit =
    macro TileMacros.intForeach_impl

  def foreachDouble(f: (Int, Int, Double) => Unit): Unit =
    macro TileMacros.doubleForeach_impl
}
