package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.io._


import geotrellis._
import geotrellis.testkit._
import geotrellis.raster.op._
import geotrellis.statistics._
import geotrellis.feature._

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
    pickle(Point(0,0, None))
    pickle(Polygon( (1,9) :: (1,6) :: (4,6) :: (4,9) :: (1,9) :: Nil, None ))
  }

  test("Tile Rasters are serializable") {
    pickle(run(io.LoadRaster("mtsthelens_tiled")))
  }

  def pickle(o:AnyRef) = {
    val stream = new ObjectOutputStream(new ByteArrayOutputStream())
    stream.writeObject(o)
  } 
}
