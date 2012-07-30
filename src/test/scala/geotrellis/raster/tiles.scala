package geotrellis.raster

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.process.TestServer
import geotrellis.raster.op.tiles._

import geotrellis.Implicits._

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


  // this is a normal raster
  val raster = Raster(data, g)

  // this is a tiled raster
  val layout = TileLayout(3, 3, 2, 2)
  val tiles = Array(r00, r10, r20, r01, r11, r21, r02, r12, r22)
  val tileData = TileArrayRasterData(tiles, layout)
  val tileRaster = Raster(tileData, g)

  val server = TestServer()
  
  // this is a really big normal raster
  val largeData = Array.fill(largeSize * largeSize)(0)
  val largeRaster = Raster(largeData, gLarge)

  describe("TileRasterData") {

    it("should return the correct values") {
      for (y <- 0 to 4; x <- 0 to 4) {
        try {
          val z = tileRaster.get(x, y)
          z must be === ((y * 5) + x) + 1
        } catch {
          case _ => println("tile(%s,%s) exploded" format (x, y))
        }
      }
    }
    it ("should load tiles given an extent & loader") {
      // Given a map extent and a loader, we can create a Raster with some tiles loaded.
      // Loading tiles (0,1) & (1,1)
      val extent = Extent(1, 21, 79, 59)
      val raster3 = Raster(tileData, g)
      for (y <- 2 to 3; x <- 0 to 3) {
        raster3.get(x, y) must be === ((y * 5) + x) + 1
      }
    }
  }

  describe("Tiler") {
    it ("should build a tiled raster from a source raster") {
      val tileRaster2 = Tiler.createTiledRaster(raster, 2, 2)
      //println(raster.asciiDraw)
      //println(tileRaster2.asciiDraw)

      for (y <- 0 to 4; x <- 0 to 4) {
        //println("x=%s y=%s" format (x, y))
        tileRaster2.get(x, y) must be === ((y * 5) + x) + 1
      }

      val trd = tileRaster2.data.asInstanceOf[TileArrayRasterData]

      val e00_test = trd.tiles(0).rasterExtent.extent;
      val e11_test = trd.tiles(4).rasterExtent.extent;
      val e22_test = trd.tiles(8).rasterExtent.extent;
      e00_test must be === e00
      e11_test must be === e11
      e22_test must be === e22
    }

    it("can be built from in-memory tiles") {
      val s = TestServer()
      val extent = Extent(1, 21, 79, 59)
      val raster4 = Raster(tileData, g)
      for (y <- 2 to 3; x <- 0 to 3) {
        raster4.get(x, y) must be === ((y * 5) + x) + 1
      }
    }

    it("can write and read tiles to disk") {
      val trd = Tiler.createTiledRasterData(raster, 2, 2)
      Tiler.writeTiles(trd, raster.rasterExtent, "testraster", "/tmp/foo")

      val s = TestServer()
      val extent = Extent(1, 21, 79, 59)
      val tileSetRD = TileSetRasterData("/tmp/foo", "testraster", TypeInt, layout, s)

      val raster4 = Raster(tileSetRD, g)
      for (y <- 0 to 3; x <- 0 to 3) {
        val expected = ((y * 5) + x) + 1
        raster4.get(x, y) must be === ((y * 5) + x) + 1
      }

      val raster5 = Raster.loadTileSet("/tmp/foo", server)
      for (y <- 0 to 3; x <- 0 to 3) {
        val expected = ((y * 5) + x) + 1
        raster5.get(x, y) must be === ((y * 5) + x) + 1
      }

      val raster6 = server.run(io.LoadTileSet("/tmp/foo"))
      for (y <- 0 to 3; x <- 0 to 3) {
        val expected = ((y * 5) + x) + 1
        raster6.get(x, y) must be === ((y * 5) + x) + 1
      }
    }

    it("can delete tiles from the disk") {
      val trd = Tiler.createTiledRasterData(raster, 2, 2)
      Tiler.deleteTiles(trd, "testraster", "/tmp/foo")
    }
  }
  
  describe("TileMin(r)") {
    it("finds the minimum value of a tiled raster") {
      val op = TileMin(tileRaster)
      val z = server.run(op)
      z must be === 1
    }
  }

  describe("TileMax(r)") {
    it("finds the maximum value of the tiled raster") {
      val op = TileMax(tileRaster)
      val z = server.run(op)
      z must be === 25
    }
  }

  describe("TileHistogram(r)") {
    it("finds the histogram of the tiled raster") {
      val op = TileHistogram(tileRaster)
      val h = server.run(op)
      //println(h.toJSON)
      h.getItemCount(0) must be === 0
      h.getItemCount(1) must be === 1
      h.getItemCount(25) must be === 1
      h.getItemCount(26) must be === 0
    }
  }

  describe("TileMax(r * 2)") {
    it("finds the maximum value") {
      val op = TileMax(tileRaster * 2)
      val z = server.run(op)
      z must be === 50
    }
  }

  describe("TileMax(r + r)") {
    it("finds the maximum value") {
      val op = TileMax(tileRaster + tileRaster)
      val z = server.run(op)
      z must be === 50
    }
  }
}
