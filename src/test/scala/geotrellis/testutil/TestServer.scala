package geotrellis.testutil

import geotrellis._
import geotrellis.raster.op._
import geotrellis.process._

import org.scalatest.{BeforeAndAfter,Suite}
import org.scalatest.matchers._

object TestServer {
  lazy val server:Server = new Server("testutil", Catalog.fromPath("src/test/resources/catalog.json"))
}
/*
 * Mixin to provide a server that is reset for each test
 */
trait TestServer extends Suite with BeforeAndAfter with ShouldMatchers {
  val server = TestServer.server 

  def run[T:Manifest](op:Op[T]):T = server.run(op)
  def runSource[T:Manifest](src:DataSource[T,_]):T = server.runSource(src)
  def getResult[T:Manifest](op:Op[T]) = server.getResult(op)

  def get(name:String) = io.LoadRaster(name)

  def assertEqual(r:Op[Raster],arr:Array[Int]) = {
    run(r).toArray should equal (arr)
  }

  def assertEqual(rd:RasterData,arr:Array[Int]) = {
    (rd.cols * rd.rows) should be (arr.length)
    for(col <- 0 until rd.cols) {
      for(row <- 0 until rd.rows) {
        rd.get(col,row) should be (arr(row*rd.cols + col))
      }
    }
  }

  def assertEqual(r:Op[Raster],r2:Op[Raster]):Unit = assertEqual(r,r2,0.0000000001)

  def assertEqual(rOp1:Op[Raster],rOp2:Op[Raster],threshold:Double):Unit = {
    val r1 = run(rOp1)
    val r2 = run(rOp2)
    
    r1.rasterExtent should be (r2.rasterExtent)

    r1.cols should be (r2.cols)
    r1.rows should be (r2.rows)
    r1.isFloat should be (r2.isFloat)

    val isFloat = r1.isFloat
    for(col <- 0 until r1.cols) {
      for(row <- 0 until r1.rows) {
        if(isFloat) {
          val v1 = r1.getDouble(col,row)
          val v2 = r2.getDouble(col,row)

          if(math.abs(v1 - v2) >= threshold) {
            println(s"Failure at (${col},${row}) - V1: $v1  V2: $v2")
            r1.getDouble(col,row) should be (r2.getDouble(col,row))
          }
        } else {
          val v1 = r1.get(col,row)
          val v2 = r2.get(col,row)
          if(v1 != v2) {
            println(s"Failure at (${col},${row}) - V1: $v1  V2: $v2")
            r1.get(col,row) should be (r2.get(col,row))
          }
        }
      }
    }
  }
}
