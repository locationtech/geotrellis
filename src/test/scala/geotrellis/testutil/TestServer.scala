package geotrellis.testutil

import geotrellis._
import geotrellis.raster.op._
import geotrellis.process._
import org.scalatest.{BeforeAndAfter,Suite}
import org.scalatest.matchers._
import geotrellis.source.DataSource

object TestServer {
  lazy val server:Server = new Server("testutil", Catalog.fromPath("src/test/resources/catalog.json"))
}
/*
 * Mixin to provide a server that is reset for each test
 */
trait TestServer extends Suite with BeforeAndAfter with ShouldMatchers {
  val server = TestServer.server 

  def run[T:Manifest](op:Op[T]):T = server.run(op)
  def runSource[T:Manifest](src:DataSource[_,T]):T = server.runSource(src)
  def getSource[T:Manifest](src:DataSource[_,T]):CalculationResult[T] = server.getSource(src)
  def getResult[T:Manifest](op:Op[T]) = server.getResult(op)

  def get(name:String) = io.LoadRaster(name)

  def assertEqual(r:Op[Raster],arr:Array[Int]):Unit =
    assertEqual(run(r),arr)

  def assertEqual(r:Raster,arr:Array[Int]):Unit = {
    (r.cols * r.rows) should be (arr.length)
    for(row <- 0 until r.rows) {
      for(col <- 0 until r.cols) {
        withClue(s"Value at ($col,$row) are not the same") {
          r.get(col,row) should be (arr(row*r.cols + col))
        }
      }
    }
  }

  def assertEqual(rd:RasterData,arr:Array[Int]) = {
    (rd.cols * rd.rows) should be (arr.length)
    for(row <- 0 until rd.rows) {
      for(col <- 0 until rd.cols) {
        withClue(s"Value at ($col,$row) are not the same") {
          rd.get(col,row) should be (arr(row*rd.cols + col))
        }
      }
    }
  }

  def assertEqual(r:Op[Raster],arr:Array[Double]):Unit = 
    assertEqual(r,arr,0.0000000001)

  def assertEqual(r:Op[Raster],arr:Array[Double],threshold:Double):Unit = {
    val raster = run(r)
    val cols = raster.rasterExtent.cols
    val rows = raster.rasterExtent.rows
    for(row <- 0 until rows) {
      for(col <- 0 until cols) {
        val v = raster.getDouble(col,row)
        if(isNaN(v)) {
          withClue(s"Value at ($col,$row) are not the same: value was ${arr(row*cols+col)}") {
            isNaN(arr(row*cols + col)) should be (true)
          }
        } else {
          withClue(s"Value at ($col,$row) are not the same:") {
            v should be (arr(row*cols + col) plusOrMinus threshold)
          }
        }
      }
    }
  }


  def assertEqual(r:Op[Raster],r2:Op[Raster]):Unit = assertEqual(r,r2,0.0000000001)

  def assertEqual(rOp1:Op[Raster],rOp2:Op[Raster],threshold:Double):Unit = {
    val r1 = run(rOp1)
    val r2 = run(rOp2)
    
    withClue("Raster extends are not equal") { r1.rasterExtent should be (r2.rasterExtent) }

    withClue("Columns are not equal") { r1.cols should be (r2.cols) }
    withClue("Rows are not equal") { r1.rows should be (r2.rows) }
    withClue("isFloat properties are not equal") { r1.isFloat should be (r2.isFloat) }

    val isFloat = r1.isFloat
    for(col <- 0 until r1.cols) {
      for(row <- 0 until r1.rows) {
        if(isFloat) {
          val v1 = r1.getDouble(col,row)
          val v2 = r2.getDouble(col,row)

          if(math.abs(v1 - v2) >= threshold) {
            withClue(s"Failure at (${col},${row}) - V1: $v1  V2: $v2") {
              r1.getDouble(col,row) should be (r2.getDouble(col,row))
            }
          }
        } else {
          val v1 = r1.get(col,row)
          val v2 = r2.get(col,row)
          if(v1 != v2) {
            withClue(s"Failure at (${col},${row}) - V1: $v1  V2: $v2") {
              r1.get(col,row) should be (r2.get(col,row))
            }
          }
        }
      }
    }
  }
}
