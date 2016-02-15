package geotrellis.raster.op.hydrology

import geotrellis.raster.op.focal.Square
import geotrellis.raster.{Tile, MethodExtensions}

trait HydrologyMethods extends MethodExtensions[Tile] {
  def accumulation(): Tile = Accumulation(self)

  /**  Operation to compute a flow direction raster from an elevation raster
    * @see [[FlowDirection]]
    */
  def flowDirection(): Tile = FlowDirection(self)

  /** Fills sink values in a raster. Returns a Tile of DoubleConstantNoDataCellType
    * @see [[Fill]]
    */
  def fill(threshold: Double): Tile = Fill(self, Square(1), None, threshold)
}
