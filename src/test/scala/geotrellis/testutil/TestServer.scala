package geotrellis.testutil

import geotrellis._
import geotrellis.raster.op._
import geotrellis.process._

import org.scalatest.{BeforeAndAfter,Suite}
import org.scalatest.matchers._

/*
 * Mixin to provide a server that is reset for each test
 */
trait TestServer extends Suite with BeforeAndAfter with ShouldMatchers {
  var server:Server = null

  before {
    server = geotrellis.process.TestServer("src/test/resources/catalog.json")  
  }

  after {
    server.shutdown()
    server = null
  }

  def run[T:Manifest](op:Op[T]):T = server.run(op)

  def get(name:String) = io.LoadRaster(name)

  def assertEqual(r:Op[Raster],arr:Array[Int]) = {
    run(r).toArray should equal (arr)
  }

  def assertEqual(r:Op[Raster],r2:Op[Raster]) = run(AssertAreEqual(Force(r),Force(r2),0.0000000001))
  def assertEqual(r:Op[Raster],r2:Op[Raster],threshold:Double) = run(AssertAreEqual(Force(r),Force(r2),threshold))
}


