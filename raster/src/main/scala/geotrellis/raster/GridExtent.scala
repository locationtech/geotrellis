package geotrellis.raster

import geotrellis.vector.Extent

case class GridExtent(extent: Extent, cellwidth: Double, cellheight: Double) extends GridDefinition

object GridExtent {
  def apply(extent: Extent, cellSize: CellSize): GridExtent =
    GridExtent(extent, cellSize.width, cellSize.height)

  implicit def gridDefinitionToGridExtent(gridDefinition: GridDefinition): GridExtent =
    gridDefinition.toGridExtent
}
