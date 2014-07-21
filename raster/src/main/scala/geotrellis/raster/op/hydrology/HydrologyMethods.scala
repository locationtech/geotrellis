package geotrellis.raster.op.hydrology

import geotrellis.raster.op.focal.Square
import geotrellis.raster.{Tile, TileMethods}

trait HydrologyMethods extends TileMethods {
  def accumulation() = Accumulation(tile)

  /**  Operation to compute a flow direction raster from an elevation raster
    * @see [[FlowDirection]]
    */
  def flowDirection() = FlowDirection(tile)

  /** Fills sink values in a raster. Returns a Tile of TypeDouble
    * @see [[Fill]]
    */
  def fill(threshold: Double): Tile = Fill(tile, Square(1), None, threshold).execute()
}
