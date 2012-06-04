package geotrellis.raster

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.process.TestServer
import geotrellis.operation._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TileSpec extends Spec with MustMatchers {
  val e = Extent(0.0, 0.0, 100.0, 100.0)
  val g = RasterExtent(e, 20.0, 20.0, 5, 5)
  
  var largeSize = 6100
  val gLarge = RasterExtent(e, 20.0, 20.0, largeSize, largeSize)

  // The extent of the test raster is xmin: 0, xmax: 100, ymin: 0, xmax: 100
  // cellsize is 20.  so (0,0) (centerpoint) is at 10,90  
  // and (5,5) is at (90,10).  (as the index goes up, x increases, y decreases)

  val data = Array(
    1, 2, 3, 4, 5,
    6, 7, 8, 9, 10,
    11, 12, 13, 14, 15,
    16, 17, 18, 19, 20,
    21, 22, 23, 24, 25)

  // Data for tiles from a tileset with 2 pixel tiles
  // tile (0,0)
  val d00 = Array(1, 2, 6, 7)
  val e00 = Extent(0, 60, 40, 100)
  val g00 = RasterExtent(e00, 20.0, 20.0, 2, 2)
  val r00 = Raster(d00, g00)

  // tile (1,0)
  val d10 = Array(3, 4, 8, 9);
  val e10 = Extent(40, 60, 80, 100)
  val g10 = RasterExtent(e10, 20.0, 20.0, 2, 2)
  val r10 = Raster(d10, g10)

  // tile (2,0)
  val d20 = Array(5, NODATA, 10, NODATA)
  val e20 = Extent(80, 60, 120, 100)
  val g20 = RasterExtent(e20, 20.0, 20, 2, 2)
  val r20 = Raster(d20, g10)

  // tile (0,1)
  val d01 = Array(11, 12, 16, 17)
  val e01 = Extent(0, 20, 40, 60)
  val g01 = RasterExtent(e01, 20.0, 20.0, 2, 2)
  val r01 = Raster(d01, g10)

  // tile (1,1)
  val d11 = Array(13, 14, 18, 19)
  val e11 = Extent(40, 20, 80, 60)
  val g11 = RasterExtent(e11, 1.0, 1.0, 2, 2)
  val r11 = Raster(d11, g10)

  // tile (2,1)
  val d21 = Array(15, NODATA, 20, NODATA)
  val e21 = Extent(80, 20, 120, 60)
  val g21 = RasterExtent(e21, 1.0, 1.0, 2, 2)
  val r21 = Raster(d21, g10)

  // tile (0,2)
  val d02 = Array(21, 22, NODATA, NODATA)
  val e02 = Extent(0, -20, 40, 20)
  val g02 = RasterExtent(e02, 20.0, 20.0, 2, 2)
  val r02 = Raster(d02, g11)

  // tile (1,2)
  val d12 = Array(23, 24, NODATA, NODATA)
  val e12 = Extent(40, -20, 80, 20)
  val g12 = RasterExtent(e12, 20.0, 20.0, 2, 2)
  val r12 = Raster(d12, g12)

  // tile (2,2)
  val d22 = Array(25, NODATA, NODATA, NODATA)
  val e22 = Extent(80, -20, 120, 20)
  val g22 = RasterExtent(e22, 20.0, 20.0, 2, 2)
  val r22 = Raster(d22, g10)

  val tiles = Array(r00, r10, r20, r01, r11, r21, r02, r12, r22) map { Option(_) }
  val raster = Raster(data, g)

  val tileset = TileSet(g, 2)
  val tileData = TileRasterData(tileset, tiles)
  val tileRaster = Raster(tileData, g)

  val server = TestServer()
  
  val largeData = Array.fill(largeSize * largeSize)(0)
  val largeRaster = Raster(largeData, gLarge)

  def loader(col: Int, row: Int): Option[Raster] = {
    val r = tiles(row * 3 + col)
    r
  }

  describe("TileRasterData") {

    it("should return the correct values") {
      for (y <- 0 to 4; x <- 0 to 4)
        tileRaster.get(x, y) must be === ((y * 5) + x) + 1

    }
    it ("should load tiles given an extent & loader") {
      // Given a map extent and a loader, we can create a Raster with some tiles loaded.
      // Loading tiles (0,1) & (1,1)
      val extent = Extent(1, 21, 79, 59)
      val tileRasterData = TileRasterData(tileset, extent, loader)
      val raster3 = Raster(tileRasterData, g)
      for (y <- 2 to 3; x <- 0 to 3) {
        raster3.get(x, y) must be === ((y * 5) + x) + 1
      }
    }
  }

  describe("Tiler") {
    it ("should build a tiled raster from a source raster") {
      val tileRaster2 = Tiler.createTileRaster(raster, 2)

      for (y <- 0 to 4; x <- 0 to 4)
        tileRaster2.get(x, y) must be === ((y * 5) + x) + 1

      val trd = tileRaster2.data.asInstanceOf[TileRasterData]

      val e00_test = trd.rasters(0).get.rasterExtent.extent;
      val e11_test = trd.rasters(4).get.rasterExtent.extent;
      val e22_test = trd.rasters(8).get.rasterExtent.extent;
      e00_test must be === e00
      e11_test must be === e11
      e22_test must be === e22
    }

    it("can write tiles to disk") {
      val trd = Tiler.createTileRasterData(raster, 2)
      Tiler.writeTiles(trd, "testraster", "/tmp")
    }
    it("can provide a loader to read tiles from disk") {
      val s = TestServer()
      val extent = Extent(1, 21, 79, 59)
      val loader = Tiler.makeTileLoader("testraster", "/tmp", s)
      val tileRasterData = TileRasterData(tileset, extent, loader)
      val raster4 = Raster(tileRasterData, g)
      for (y <- 2 to 3; x <- 0 to 3) {
        raster4.get(x, y) must be === ((y * 5) + x) + 1
      }
    }
    it("can delete tiles from the disk") {
      val trd = Tiler.createTileRasterData(raster, 2)
      Tiler.deleteTiles(trd, "testraster", "/tmp")
    }
  }
  
  describe("DoTile") {

    it("can operate over each subraster of a tiled raster") {
      val op = ForEachTile(Literal(tileRaster))(AddConstant(_, 3))
      val result = server.run(op)
      for (y <- 0 to 4; x <- 0 to 4)
        result.get(x, y) must be === ((y * 5) + x) + 1 + 3
    }

    it ("can operate on raster larger than 6000x6000") {

      val largeTileRaster = Tiler.createTileRaster(largeRaster, 1024)

      val start = System.currentTimeMillis
      val op = ForEachTile(Literal(largeTileRaster))(AddConstant(_, 3))
      val result = server.run(op)
      val elapsed = System.currentTimeMillis - start

    }
  }
}
