package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

// object TmsTilingScheme {
//   val DEFAULT_TILE_SIZE = 512

//   def apply(extent: Extent): TmsTilingScheme = 
//     apply(extent, DEFAULT_TILE_SIZE)

//   def apply(crs: CRS): TmsTilingScheme =
//     apply(crs, DEFAULT_TILE_SIZE)

//   def apply(crs: CRS, tileSize: Int): TmsTilingScheme = {
//     val extent = {
//       val ll = Extent(-180, -90, 179.99999, 89.99999)
//       if(crs != LatLng) { ll.reproject(LatLng, crs) } else ll
//     }
//     TmsTilingScheme(extent, tileSize)
//   }

//   def tileCols(level: Int): Int = math.pow(2, level).toInt
//   def tileRows(level: Int): Int = math.pow(2, level - 1).toInt
// }

// case class TmsTilingScheme(extent: Extent, tileSize: Int) extends TilingScheme {
//   private def zoom(res: Double, tileSize: Int): Int = {
//     val resWithEp = res + 0.00000001

//     for(i <- 1 to 20) {
//       val resolution = 360 / (TmsTilingScheme.tileCols(i) * tileSize).toDouble
//       if(resWithEp >= resolution)
//         return i
//     }
//     return 0
//   }

//   /** TODO: Improve this algorithm */
//   def zoomLevelFor(cellSize: CellSize): ZoomLevel = {
//     val l =
//       math.max(
//         zoom(cellSize.width, tileSize),
//         zoom(cellSize.height, tileSize)
//       )
//     zoomLevel(l)
//   }

//   def zoomLevel(l: Int): ZoomLevel =
//     new TmsZoomLevel(l, tileSize, extent)
// }

// class TmsZoomLevel(val level: Int, tileSize: Int, val extent: Extent) extends ZoomLevel {
//   val tileCols = TmsTilingScheme.tileCols(level)
//   val tileRows = TmsTilingScheme.tileRows(level)
//   val pixelCols = tileSize
//   val pixelRows = tileSize

//   def tileId(tcol: Int, trow: Int): TileId = 
//     (trow * tileCols) + tcol

//   def tileCoord(tileId: TileId): TileCoord = {
//     val trow = tileId / tileCols
//     val tcol = tileId - (trow * tileCols)
//     (tcol.toInt, trow.toInt)
//   }
// }
