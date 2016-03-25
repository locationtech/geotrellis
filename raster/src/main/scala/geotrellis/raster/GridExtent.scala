package geotrellis.raster

import geotrellis.vector.Extent


/**
  * The [[GridExtent]] type.
  */
case class GridExtent(extent: Extent, cellwidth: Double, cellheight: Double) extends GridDefinition

/**
  * The companion object for the [[GridExtent]] type.
  */
object GridExtent {

  /**
    * Given an [[Extent]] and a [[CellSize]], produce a new
    * [[GridExtent]].
    */
  def apply(extent: Extent, cellSize: CellSize): GridExtent =
    GridExtent(extent, cellSize.width, cellSize.height)


  /**
    * An implicit function to convert a [[GridDefinition]] to a
    * [[GridExtent]].
    */
  implicit def gridDefinitionToGridExtent(gridDefinition: GridDefinition): GridExtent =
    gridDefinition.toGridExtent
}
