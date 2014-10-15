package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

// object TilingScheme {
//   /** Default tiling scheme for WSG84 */
//   def TMS: TilingScheme =
//     TMS(TmsTilingScheme.DEFAULT_TILE_SIZE)
//   def TMS(tileSize: Int): TilingScheme = 
//     TmsTilingScheme(tileSize)
// }

// trait TilingScheme {
//   def layoutFor(crs: CRS, cellSize: CellSize): LayoutLevel =
//     layoutFor(crs.worldExtent, cellSize)

//   def layoutFor(extent: Extent, cellSize: CellSize): LayoutLevel

//   def level(id: Int): LayoutLevel
// }
