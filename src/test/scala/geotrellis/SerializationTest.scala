package geotrellis

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import java.io._


import geotrellis._
import geotrellis.testutil._
import geotrellis.raster.op._
import geotrellis.statistics._

@RunWith(classOf[JUnitRunner])
class SerializationTest extends FunSuite 
                        with ShouldMatchers 
                        with RasterBuilders 
                        with TestServer {

  // Operations and data objects that may be sent remotely must be serializable.
  test("Operation and data object serialization test") {
    pickle(Literal(1))
    pickle(byteRaster)
    val addOp = local.Add(byteRaster, 1)
    pickle(addOp)
    pickle(local.Add(addOp, 2))
    pickle(FastMapHistogram())
    pickle(Statistics(0,0,0,0,0,0))
  }

  test("Tile Rasters are serializable") {
    pickle(run(io.LoadRaster("sbn_tiled")))
  }

  def pickle(o:AnyRef) = {
    val stream = new ObjectOutputStream(new ByteArrayOutputStream())
    stream.writeObject(o)
  } 
}
