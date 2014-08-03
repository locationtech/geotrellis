package geotrellis.raster

/** A lighweight wrapper around performing foreach calculations on a set of cell coordinates */
trait CellSet {
  /** Calls a funtion with the col and row coordinate of every cell contained in the CellSet.
   *
   * @param    f      Function that takes col and row coordinates, that will be called
   *                  for each cell contained in this CellSet.
   */
  def foreach(f: (Int, Int)=>Unit): Unit
}
