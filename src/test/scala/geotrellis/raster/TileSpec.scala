package geotrellis.raster

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op.tiles._
import geotrellis.statistics.op._
import geotrellis.testutil._
import geotrellis.Implicits._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TileSpec extends FunSpec with MustMatchers 
                               with TestServer
                               with RasterBuilders {
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
          case _:Throwable => println("tile(%s,%s) exploded" format (x, y))
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

      for (y <- 0 to 4; x <- 0 to 4) {
        tileRaster2.get(x, y) must be === ((y * 5) + x) + 1
      }

      val trd = tileRaster2.data.asInstanceOf[TileArrayRasterData]

      val e00_test = trd.tiles(0).rasterExtent.extent;
      val e11_test = trd.tiles(4).rasterExtent.extent;
      val e22_test = trd.tiles(8).rasterExtent.extent;
      e00_test must be === e00
      e11_test must be === e11
      e22_test must be === e22

      tileRaster2.isTiled must be === true
    }

    it("should have raster extent and tile layout length consistent when building in memory") {
      val arr = Array(0,1,2,3,4,
                      1,1,2,3,4,
                      2,1,2,3,4,
                      3,1,2,3,4)
      val r = Tiler.createTiledRaster(createRaster(arr),2,2)
      val data = r.data.asInstanceOf[TiledRasterData]
      data.length should be (r.cols * r.rows)
    }

    it("should have raster extent and tile layout length consistent when building in memory for large raster") {
      val arr = (for(col <- 0 until 979; row <- 0 to 1400) yield { row + 979*col }).toArray
      val r = Raster(IntArrayRasterData(arr,979,1400),RasterExtent(Extent(0,0,979,1400),1,1,979,1400))
      val tr = Tiler.createTiledRaster(r,89,140)
      val data = tr.data.asInstanceOf[TileArrayRasterData]
      data.length should be (tr.cols * tr.rows)
    }

    it("can be built from in-memory tiles") {
      val extent = Extent(1, 21, 79, 59)
      val raster4 = Raster(tileData, g)
      for (y <- 2 to 3; x <- 0 to 3) {
        raster4.get(x, y) must be === ((y * 5) + x) + 1
      }
    }

    it("can write and read tiles to disk") {
      val trd = Tiler.createTiledRasterData(raster, 2, 2)
      Tiler.writeTiles(trd, raster.rasterExtent, "testraster", "/tmp/foo")

      val path = "/tmp/testraster.json"
      val f = new java.io.File(path)
      val layer = RasterLayer.fromFile(f) match {
        case Some(l) => l
        case None => sys.error(s"Raster layer at $path does not exist.")
      }

      val tileSetRD = layer.getRaster(None).data.asInstanceOf[TileSetRasterData]

      val raster4 = Raster(tileSetRD, g)
      for (y <- 0 to 3; x <- 0 to 3) {
        val expected = ((y * 5) + x) + 1
        raster4.get(x, y) must be === ((y * 5) + x) + 1
      }

      val raster5 = layer.getRaster
      for (y <- 0 to 3; x <- 0 to 3) {
        val expected = ((y * 5) + x) + 1
        raster5.get(x, y) must be === ((y * 5) + x) + 1
      }

      val raster6 = run(io.LoadTileSet(path))
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
  
  describe("stat.Min(r)") {
    it("finds the minimum value of a tiled raster") {
      val op = stat.Min(tileRaster)
      val z = run(op)
      z must be === 1

      // test TileSetRasterData
      val op2 = stat.Min(io.LoadRaster("mtsthelens_tiled_cached"))
      val z2 = run(op2)
      z2 must be === 2231
    }
  }

  describe("stat.Max(r)") {
    it("finds the maximum value of the tiled raster") {
      val op = stat.Max(tileRaster)
      val z = run(op)
      z must be === 25
    }
  }

  describe("stat.GetHistogram(r)") {
    it("finds the histogram of the tiled raster") {
      val op = stat.GetHistogram(tileRaster)
      val h = run(op)
      h.getItemCount(0) must be === 0
      h.getItemCount(1) must be === 1
      h.getItemCount(25) must be === 1
      h.getItemCount(26) must be === 0
    }
  }

  describe("stat.Max(r * 2)") {
    it("finds the maximum value") {
      val op = stat.Max(tileRaster * 2)
      val z = run(op)
      z must be === 50
    }
  }

  describe("stat.Max(r + r)") {
    it("finds the maximum value") {
      val op = stat.Max(tileRaster + tileRaster)
      val z = run(op)
      z must be === 50
    }
  }
  
  describe("Tile layout") {
    it("has an origin of 0,0") {
      val xmin = -180.0044642857
      val ymax = 90.00892799539997
      val worldExtent = Extent(xmin,-90.0,180.00446313369997,90.00892799539997)
      val re = RasterExtent(worldExtent, 0.0089285714,0.0089285714,40321,20161)
      val tileLayout = Tiler.buildTileLayout(re, 2048, 2048)
      val resLayout = tileLayout.getResolutionLayout(re)
      assert(resLayout.getXCoord(0) === xmin)
      assert(resLayout.getYCoord(0) === ymax)
    }
  }
  
  describe("LazyTiledWrapper") {
    val d = 10
    val r = createConsecutiveRaster(d)
    val layout = TileLayout(5,5,2,2)
    val lazyWrapper = LazyTiledWrapper(r.data,layout)
    val tile = lazyWrapper.getTile(2,2)
    val expected = Array(45,46,55,56)
    assertEqual(tile,expected)
  }
}
