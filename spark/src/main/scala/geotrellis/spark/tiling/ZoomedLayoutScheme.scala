package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.{ProjectedExtent, Extent}
import geotrellis.vector.reproject._

object ZoomedLayoutScheme {
  val DEFAULT_TILE_SIZE = 256

  def apply(tileSize: Int = DEFAULT_TILE_SIZE) =
    new ZoomedLayoutScheme(tileSize)
}
case class ZoomLayoutLevel(zoom: Int, override val extent: Extent, override val tileLayout: TileLayout)
  extends LayoutDefinition(extent, tileLayout)

/** Layout for zoom levels based off of a power-of-2 scheme,
  * used in Leaflet et al.*/
class ZoomedLayoutScheme(tileSize: Int) extends LayoutScheme {
  private def zoom(res: Double, tileSize: Int, worldSpan: Double): Int = {
    val resWithEp = res + 0.00000001

    for(i <- 1 to 20) {
      val resolution = worldSpan / (tileCols(i) * tileSize).toDouble
      if(resWithEp >= resolution)
        return i
    }
    return 0
  }

  private def tileCols(level: Int): Int = math.pow(2, level).toInt
  private def tileRows(level: Int): Int = math.pow(2, level).toInt

  /** TODO: Improve this algorithm. One improvement is to follow the algorithm
    * described in  "Tile-Based Geospatial Information Systems Principles and Practices"
    * by John T. Sample & Elias Ioup, section 3.1.2 */
  def levelFor(projectedExtent: ProjectedExtent, cellSize: CellSize) = {
    val worldExtent = projectedExtent.crs.worldExtent
    val l =
      math.max(
        zoom(cellSize.width, tileSize, worldExtent.width),
        zoom(cellSize.height, tileSize, worldExtent.height)
      )

    levelForZoom(worldExtent, l)
  }

  def levelForZoom(worldExtent: Extent, id: Int) = {
    if(id < 1)
      sys.error("TMS Tiling scheme does not have levels below 1")
    LayoutLevel(id, LayoutDefinition(worldExtent, TileLayout(tileCols(id), tileRows(id), tileSize, tileSize)))
  }

  def zoomOut(level: LayoutLevel) = {
    val layout = level.layout
    new LayoutLevel(
      zoom = level.zoom - 1,
      layout = LayoutDefinition(
        extent = layout.extent,
        layout = TileLayout(
          layout.tileLayout.layoutCols / 2,
          layout.tileLayout.layoutRows / 2,
          layout.tileLayout.tileCols,
          layout.tileLayout.tileRows
        )
      )
    )
  }

  def zoomIn(level: LayoutLevel) = {
    val layout = level.layout
    new LayoutLevel(
      zoom = level.zoom + 1,
      layout = LayoutDefinition(
        extent = layout.extent,
        layout = TileLayout(
          layout.tileLayout.layoutCols * 2,
          layout.tileLayout.layoutRows * 2,
          layout.tileLayout.tileCols,
          layout.tileLayout.tileRows
        )
      )
    )
  }
}
