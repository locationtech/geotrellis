package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DoSpec extends FunSpec 
                with ShouldMatchers 
                with TestServer 
                with RasterBuilders {
  describe("DoCell") {
    it ("performs integer function") {
      val r = positiveIntegerRaster
      val result = run(DoCell(r)( (x:Int) => x * 10))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 10)
        }
      }
    }

    it ("performs integer function against TypeDouble raster") {
      val r = probabilityNoDataRaster
      val result = run(DoCell(r)( (x:Int) => if(x == NODATA) NODATA else x * 10 ))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { java.lang.Double.isNaN(result.getDouble(col,row)) should be (true) }
          else { result.getDouble(col,row) should be (r.getDouble(col,row).toInt * 10) }
        }
      }
    }

    it ("performs double function") {
      val r = probabilityRaster
      val result = run(DoCellDouble(r)( (x:Double) => x * 10.0))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 10)
        }
      }
    }

    it ("performs double function against TypeInt raster") {
      val r = positiveIntegerNoDataRaster
      val result = run(DoCellDouble(r)( (x:Double) => x * 10.0))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { result.get(col,row) should be (NODATA) }
          else { result.get(col,row) should be (r.get(col,row).toDouble * 10.0) }
        }
      }
    }

    it ("performs binary integer function") {
      val r = positiveIntegerRaster
      val result = run(DoCell(r, r)({ (z1:Int,z2:Int) => z1+z2 }))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 2)
        }
      }
    }

    it ("performs binary integer function against TypeDouble raster") {
      val r = probabilityNoDataRaster
      val result = run(DoCell(r, r)({ (z1:Int,z2:Int) => if(z1 == NODATA) { NODATA} else { z1 + z2 } }))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { java.lang.Double.isNaN(result.getDouble(col,row)) should be (true) }
          else { result.getDouble(col,row) should be (r.getDouble(col,row).toInt * 2) }
        }
      }
    }

    it ("performs binary double function") {
      val r = probabilityRaster
      val result = run(DoCellDouble(r, r)({ (z1:Double,z2:Double) => z1+z2 }))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 2)
        }
      }
    }

    it ("performs binary double function against TypeInt raster") {
      val r = positiveIntegerNoDataRaster
      val result = run(DoCellDouble(r, r)({ (z1:Double,z2:Double) => if(z1 == NODATA) {NODATA} else {z1 + z2} }))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { result.get(col,row) should be (NODATA) }
          else { result.get(col,row) should be (r.get(col,row) * 2) }
        }
      }
    }

    it ("works with int raster with NODATA values") {
      val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)
      val nd = NODATA
      
      val data1 = Array(12, 12, 13, 14, 15,
                        44, 91, nd, 11, 95,
                        12, 13, 56, 66, 66,
                        44, 91, nd, 11, 95)

      val f2 = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
                cellsize:Double, srs:Int) => {
        val g = RasterExtent(Extent(xmin, ymin, xmin + cellsize * cols, ymin + cellsize * rows),
                             cellsize, cellsize, cols, rows)
        Raster(a, g)
      }

      val f = (a:Array[Int], cols:Int, rows:Int, xmin:Double, ymin:Double,
               cellsize:Double) => f2(a, cols, rows, xmin, ymin, cellsize, 999)
      
      val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val r = Literal(f(a, 3, 3, 0.0, 0.0, 1.0))

      val r2 = run(DoCell(r)({z:Int => z + 1}))
      val d = r2.toArray
      d should be (a.map { _ + 1 })
    }
  }
}
