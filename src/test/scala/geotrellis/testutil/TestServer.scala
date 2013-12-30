package geotrellis.testutil

import geotrellis._
import geotrellis.raster.op._
import geotrellis.process._
import org.scalatest.{BeforeAndAfter,Suite}
import org.scalatest.matchers._
import geotrellis.source.DataSource

object TestServer {
  lazy val init = {
    GeoTrellis.init(GeoTrellisConfig("src/test/resources/catalog.json"),"test-server")
  }
}

trait TestServer extends Suite with BeforeAndAfter with ShouldMatchers {
//  val server = TestServer.server
  TestServer.init

  def run[T](op:Op[T]):OperationResult[T] = GeoTrellis.run(op)
  def run[T](src:DataSource[_,T]):OperationResult[T] = GeoTrellis.run(src)
  def get[T](op:Op[T]):T = GeoTrellis.get(op)
  def get[T](src:DataSource[_,T]):T = GeoTrellis.get(src)

  def getRaster(name:String):Op[Raster] = getRaster("test:fs",name)
  def getRaster(ds:String,name:String):Op[Raster] = io.LoadRaster(ds,name)

  def assertEqual(r:Op[Raster],arr:Array[Int]):Unit =
    assertEqual(get(r),arr)

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
    val raster = get(r)
    val cols = raster.rasterExtent.cols
    val rows = raster.rasterExtent.rows
    for(row <- 0 until rows) {
      for(col <- 0 until cols) {
        val v = raster.getDouble(col,row)
        if(isNoData(v)) {
          withClue(s"Value at ($col,$row) are not the same: value was ${arr(row*cols+col)}") {
            isNoData(arr(row*cols + col)) should be (true)
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
    val r1 = get(rOp1)
    val r2 = get(rOp2)
    
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
