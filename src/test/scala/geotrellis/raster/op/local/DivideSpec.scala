package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DivideSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("Divide") {
    it("divides a constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) / 5)
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Divide(r,3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 3)
        }
      }
    }

    it("divides a constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(-10,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-10 / r.get(col,row))
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Divide(-3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.0 / r.getDouble(col,row))
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,5.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) / 5.1).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = run(Divide(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 0.3)
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = run(Divide(-10.7,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (-10.7 / r.get(col,row)).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = run(Divide(-3.3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.3 / r.getDouble(col,row))
        }
      }
    }

    it("divides an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = run(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (1)
        }
      }
    }
    
    it("divides a double raster to itself") {
      val r = probabilityRaster
      val result = run(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (1.0)
        }
      }
    }

    it("divides two tiled RasterDataSources correctly") {
      val rs1 = RasterDataSource("quad_tiled")
      val rs2 = RasterDataSource("quad_tiled2")

      val r1 = runSource(rs1)
      val r2 = runSource(rs2)
      getSource(rs1 / rs2) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until r1.rasterExtent.rows) {
            for(col <- 0 until r1.rasterExtent.cols) {
              if(result.get(col,row) == NODATA) {
                (r1.get(col,row) == NODATA ||
                 r2.get(col,row) == NODATA ||
                 r2.get(col,row) == 0) should be (true)
              } else {
                result.get(col,row) should be (r1.get(col,row) / r2.get(col,row))
              }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("divides three tiled RasterDataSources correctly") {
      val rs1 = createRasterDataSource(
        Array( 1000,1000,1000, 1000,1000,1000, 1000,1000,1000,
               1000,1000,1000, 1000,1000,1000, 1000,1000,1000,

               1000,1000,1000, 1000,1000,1000, 1000,1000,1000,
               1000,1000,1000, 1000,1000,1000, 1000,1000,1000),
        3,2,3,2)

      val rs2 = createRasterDataSource(
        Array( 200,200,200, 200,200,200, 200,200,200,
               200,200,200, 200,200,200, 200,200,200,

               200,200,200, 200,200,200, 200,200,200,
               200,200,200, 200,200,200, 200,200,200),
        3,2,3,2)

      val rs3 = createRasterDataSource(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        3,2,3,2)

      getSource(rs1 / rs2 / rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              result.get(col,row) should be ((1000/200)/2)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
