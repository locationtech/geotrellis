package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

case class LayoutLevel(id: Int, tileLayout: TileLayout){
  def up: LayoutLevel =
    LayoutLevel(id - 1,
      TileLayout(tileLayout.tileCols/2, tileLayout.tileRows/2,
        tileLayout.pixelCols/2, tileLayout.pixelRows/2)
    )

  def down: LayoutLevel =
    LayoutLevel(id + 1,
    TileLayout(tileLayout.tileCols*2, tileLayout.tileRows*2,
      tileLayout.pixelCols*2, tileLayout.pixelRows*2)
  )

}

object TilingScheme {
  /** Default tiling scheme for WSG84 */
  def TMS: TilingScheme =
    TMS(TmsTilingScheme.DEFAULT_TILE_SIZE)
  def TMS(tileSize: Int): TilingScheme = 
    TmsTilingScheme(tileSize)
}

trait TilingScheme {
  def layoutFor(crs: CRS, cellSize: CellSize): LayoutLevel =
    layoutFor(crs.worldExtent, cellSize)

  def layoutFor(extent: Extent, cellSize: CellSize): LayoutLevel

  def level(id: Int): LayoutLevel
}

