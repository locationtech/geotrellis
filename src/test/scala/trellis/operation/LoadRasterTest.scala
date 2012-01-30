package trellis.operation

import trellis._
import trellis.process._
import trellis.raster._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LoadRasterTest extends FunSuite {
  val server = TestServer("src/test/resources/catalog.json")
  //val e = Extent(0.0, 0.0, 10.0, 10.0)
  //val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  println("hi")
  test("load foo and bar") {
    val foo = server.run(LoadRaster("quadborder"))

    //println(foo)
    //val bar = LoadRaster("bar")
    //println(bar)
  }
}
