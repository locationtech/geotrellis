package geotrellis.raster.mosaic

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class MosaicSpec extends FunSpec
                         with TileBuilders
                         with RasterMatchers
                         with TestFiles {
  describe("MosaicBuilder") {
    it("should mosaic a tile split by CompositeTile back into itself") {
      val totalCols = 1000
      val totalRows = 1500
      val layoutCols = 2
      val layoutRows = 1
      val tileCols = totalCols / layoutCols
      val tileRows = totalRows / layoutRows

      if( (tileCols*layoutCols, tileRows*layoutRows) != (totalCols, totalRows) )
        sys.error("This test requirest that the total col\rows be divisible by the tile col\rows")

      val (tile: Tile, extent: Extent) = {
        val rs = loadTestArg("sbn/SBN_inc_percap")
        val (t, e) = (rs.tile, rs.extent)
        val resampled = t.resample(e, totalCols, totalRows)
        (resampled, e)
      }

      val tileLayout = TileLayout(layoutCols, layoutRows, tileCols, tileRows)

      val rasters: Seq[(Extent, Tile)] = {
        val tileExtents = TileExtents(extent, tileLayout)
        val tiles = CompositeTile.wrap(tile, tileLayout).tiles
        tiles.zipWithIndex.map { case (tile, i) => (tileExtents(i), tile) }
      }

      val builder = new MosaicBuilder(tile.cellType, extent, tile.cols, tile.rows)

      rasters.foreach(builder += _)
      val result = builder.result.tile

      assertEqual(result, tile)
    }
  }

  describe("Merge functions") {
    it("should merge values from overlapping extents") {
      val tiles = Array(
        Extent(0,4,4,8) -> IntArrayTile.fill(0,4,4),
        Extent(4,4,8,8) -> IntArrayTile.fill(1,4,4),
        Extent(0,0,4,4) -> IntArrayTile.fill(2,4,4),
        Extent(4,0,8,4) -> IntArrayTile.fill(3,4,4)
      )

      val extent = Extent(2,2,6,6)
      val mergeTile = ArrayTile.empty(TypeInt, 4,4)

      for ( (ex, tile) <- tiles) {
        mergeTile.merge(extent, ex, tile)
      }
      val expected = ArrayTile(Array(
        0,0,1,1,
        0,0,1,1,
        2,2,3,3,
        2,2,3,3), 4, 4)

      mergeTile should equal (expected)
    }
  }
}
