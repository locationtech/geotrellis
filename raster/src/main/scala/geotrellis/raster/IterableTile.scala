package geotrellis.raster

import geotrellis.macros._

/**
  * Trait to supply foreach(|Double) methods.
  */
trait IterableTile extends MacroIterableTile {

  /**
    * Execute the given function 'f' at every location in the tile.
    * The function 'f' takes the column, row, and value and returns
    * nothing (presumably for side-effects).
    */
  def foreach(f: (Int, Int, Int) => Unit): Unit =
    macro TileMacros.intForeach_impl

  /**
    * Execute the given function 'f' at every location in the tile.
    * The function 'f' takes the column, row, and value, the last one
    * as a double, and returns nothing (presumably for side-effects).
    */
  def foreachDouble(f: (Int, Int, Double) => Unit): Unit =
    macro TileMacros.doubleForeach_impl
}
