package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LoadRasterTest extends FunSuite {
  val server = TestServer.server

  test("load valid raster") {
    server.run(io.LoadRaster("quadborder"))
  }

  //test("fail to load invalid raster") {
  //  val result = try {
  //    server.run(LoadRaster("does-not-exIST"))
  //    "ok"
  //  } catch {
  //    case _ => "exception"
  //  }
  //  assert(result === "exception")
  //}
}
