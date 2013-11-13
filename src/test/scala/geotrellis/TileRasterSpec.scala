package geotrellis

import geotrellis.raster._
import geotrellis.process._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

class TileRasterSpec extends FunSpec 
                        with MustMatchers 
                        with RasterBuilders
                        with TestServer {
  describe("wrap") {
    it("wraps a literal raster") {
      val r = 
        createRaster(
          Array( 1,1,1, 2,2,2, 3,3,3,
                 1,1,1, 2,2,2, 3,3,3,
               
                 4,4,4, 5,5,5, 6,6,6,
                 4,4,4, 5,5,5, 6,6,6 ),
          9, 4)

      val tl = TileLayout(3,2,3,2)
      val tiled = TileRaster.wrap(r,tl)
      val tiles = tiled.tiles

      tiles.length should be (6)
      
      val values = collection.mutable.Set[Int]()
      for(tile <- tiles) {
        tile.rasterExtent.cols should be (3)
        tile.rasterExtent.rows should be (2)
        val arr = tile.toArray
        arr.toSet.size should be (1)
        values += arr(0)
      }
      values.toSeq.sorted.toSeq should be (Seq(1,2,3,4,5,6))
      
      assertEqual(r,tiled)
    }

    it("splits up a loaded raster") {
      val rOp = get("elevation")
      val tOp = 
        rOp.map { r =>
          val (tcols,trows) = (11,20)
          val pcols = r.rasterExtent.cols / tcols
          val prows = r.rasterExtent.rows / trows
          val tl = TileLayout(tcols,trows,pcols,prows)
          TileRaster.wrap(r,tl)
        }
      val tiled = run(tOp)
      val raster = run(rOp)

      assertEqual(tiled,raster)
    }
  }
}
