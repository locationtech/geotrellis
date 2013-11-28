package geotrellis.raster

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class RasterDataSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("convert") {
    it("should convert a byte raster to an int raster") { 
      val r = byteRaster
      var result = r.convert(TypeShort)
                    .map { z => z + 100 }
      result.rasterType should be (TypeShort)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 100)
        }
      }
    }
  }

  describe("mapIfSet") {
    def check(r:Raster) = {
      val r2 = r.mapIfSet(z => 1)
      val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val v = r.get(col,row)
          val v2 = r2.get(col,row)
          if(isNoData(v)) {
            v2 should be (NODATA)
          }
        }
      }

      val r3 = r.mapIfSetDouble(z => 1.0)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val v = r.getDouble(col,row)
          val v3 = r3.getDouble(col,row)
          if(isNoData(v)) {
            isNoData(v3) should be (true)
          }
        }
      }
    }

    it("should respect NoData values") {
      withClue("ByteArrayRasterData") { check(byteNoDataRaster) }
      withClue("ShortArrayRasterData") { 
        val n = shortNODATA
        check(createRaster(Array[Short](1,2,3,n,n,n,3,4,5)))
      }
      withClue("IntArrayRasterData") { check(positiveIntegerNoDataRaster) }
      withClue("FloatArrayRasterData") { 
        val n = Float.NaN
        check(createRaster(Array[Float](1.0f,2.0f,3.0f,n,n,n,3.0f,4.0f,5.0f)))
      }
    }
  }
}
