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

  test("load valid raster") {
    server.run(LoadRaster("quadborder"))
  }

  test("fail to load invalid raster") {
    val result = try {
      server.run(LoadRaster("does-not-exIST"))
      "ok"
    } catch {
      case _ => "exception"
    }
    assert(result === "exception")
  }
}
