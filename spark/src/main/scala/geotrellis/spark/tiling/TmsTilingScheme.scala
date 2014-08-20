package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._

object TmsTilingScheme {
  val DEFAULT_TILE_SIZE = 512

  def apply(tileSize: Int = DEFAULT_TILE_SIZE) =
    new TilingScheme {
      private def zoom(res: Double, tileSize: Int, worldSpan: Double): Int = {
        val resWithEp = res + 0.00000001

        for(i <- 1 to 20) {
          val resolution = worldSpan / (tileCols(i) * tileSize).toDouble
          if(resWithEp >= resolution)
            return i
        }
        return 0
      }

      def tileCols(level: Int): Int = math.pow(2, level).toInt
      def tileRows(level: Int): Int = math.pow(2, level - 1).toInt

      /** TODO: Improve this algorithm */
      def layoutFor(extent: Extent, cellSize: CellSize): LayoutLevel = {
        val l =
          math.max(
            zoom(cellSize.width, tileSize, extent.width),
            zoom(cellSize.height, tileSize, extent.height)
          )

        level(l)
      }

      def level(id: Int): LayoutLevel = {
        if(id < 1) 
          sys.error("TMS Tiling scheme does not have levels below 1")

        LayoutLevel(id, TileLayout(tileCols(id), tileRows(id), tileSize, tileSize))
      }
    }
}
