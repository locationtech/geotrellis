package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.process._
import geotrellis.data._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HillshadeTest extends FunSuite {
  val server = TestServer()

  def grayscale(n:Int) = (1 to 128).map {
    i => (i * n, ((i * 65536 * 2 + i * 256 * 2 + i * 2) << 8) | 255)
  }.toArray

  def time() = System.currentTimeMillis()

  // for more information on how hillshade work, see: http://bit.ly/Qj0YPg.
  // note that we scale by 128 not 256, so our result is 77 instead of 154.
  test("esri hillshade") {
    val re = RasterExtent(Extent(0.0, 0.0, 25.0, 25.0), 5.0, 5.0, 5, 5)
    val arr = Array(0, 0, 0, 0, 0,
                    0, 2450, 2461, 2483, 0,
                    0, 2452, 2461, 2483, 0,
                    0, 2447, 2455, 2477, 0,
                    0, 0, 0, 0, 0)
    val data = IntArrayRasterData(arr, 5, 5)
    val raster = Raster(data, re)

    val h = server.run(focal.Hillshade(raster))
    assert(h.get(2, 2) === 77)
  }

  test("hillshade") {
    val path = "/Users/erik/elevation.arg"
  
    println("Starting test Hillshade operation.")
    println("Loading raster")
    val server = TestServer()
  
    val t0 = time()
    val raster = server.run(io.LoadFile(path))
    println("loaded raster in %s ms" format (time() - t0))
  
    server.run(io.WritePNGFile(raster, "/tmp/raster.png", grayscale(6), 0, true))
    println("raster: " + server.run(stat.GetHistogram(raster)).toJSON)
  
    val t1 = System.currentTimeMillis
    val h1 = server.run(focal.Hillshade(raster))
    println("generated hillshade from memory in %s ms" format (time() - t1))
    server.run(io.WritePNGFile(h1, "/tmp/hillshade1.png", grayscale(1), 0, true))
  
    // this is just to second-guess the other code, in case our color
    // rendering breaks and we want something more direct.
    val h2 = h1.convert(TypeInt).mapIfSet(z => (z << 24) | (z << 16) | (z << 8) | 255)
    server.run(io.WritePNGFile2(h2, "/tmp/hillshade2.png", 0, true))
  
    println("hillshade: " + server.run(stat.GetHistogram(h1)).toJSON)
    println("  " + h1.data)
    println("  " + h1.data.getType)
  
    server.shutdown()
  }
}
