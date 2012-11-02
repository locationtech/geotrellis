package geotrellis.raster

import geotrellis._
import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import geotrellis.process.TestServer
import geotrellis.vector.op.geometry.SimplePolygon
import geotrellis.statistics.op.stat.TiledPolygonalZonalCount

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterizationSpec extends FunSpec with MustMatchers {

  val server = TestServer()
  describe("r") {
    it("should rasterize a polygon too complex for gridpolygon ") {
    //val polygonWkt = "POLYGON ((-74.6229572569999 41.5930024740001, -74.6249086829999 41.5854607480001, -74.6087045219999 41.572877582, -74.6396698609999 41.5479203780001, -74.6134071899999 41.5304959030001, -74.6248611209999 41.5210940920001, -74.6080037309999 41.510192955, -74.61917188 41.500007054, -74.6868377089999 41.5507426980001, -74.6752089579999 41.5646628200001, -74.6776005779999 41.573316585, -74.6637320329999 41.5691605160001, -74.6623717069999 41.5770289280001, -74.6558314389999 41.552671724, -74.6494842519999 41.5467347190001, -74.6459184919999 41.565179846, -74.6344289929999 41.5694043560001, -74.6229572569999 41.5930024740001))"
      //val polygon = TestServer().run(io.LoadWkt(polygonWkt))  
      val pOp3 = SimplePolygon(
        Array((-74.6229572569999, 41.5930024740001),
          (-74.6249086829999, 41.5854607480001),
          (-74.6087045219999, 41.572877582),
          (-74.6396698609999, 41.5479203780001),
          (-74.6134071899999, 41.5304959030001),
          (-74.6248611209999, 41.5210940920001),
          (-74.6080037309999, 41.510192955),
          (-74.61917188, 41.500007054),
          (-74.6868377089999, 41.5507426980001),
          (-74.6752089579999, 41.5646628200001),
          (-74.6776005779999, 41.573316585),
          (-74.6637320329999, 41.5691605160001),
          (-74.6623717069999, 41.5770289280001),
          (-74.6558314389999, 41.552671724),
          (-74.6494842519999, 41.5467347190001),
          (-74.6459184919999, 41.565179846),
          (-74.6344289929999, 41.5694043560001),
          (-74.6229572569999, 41.5930024740001)), 1)
        val p = server.run(pOp3)
        val peOp = geotrellis.vector.op.extent.PolygonExtent(pOp3)
        val newE, Extent(xmin, ymin, xmax, ymax) = server.run(peOp)
        val tileExtent = Extent( -88.57589314970001, 35.15178531379998, -70.29017892250002, 53.43749954099997)
       // val rasterExtent = RasterExtent(tileExtent, 0.008929,0.008929, 2048, 2048 )
       // val rasterExtent = RasterExtent(tileExtent, 0.008929,0.008929, 2048, 2048 )
        val rasterExtent = RasterExtent(tileExtent, 0.0178571428,0.0178571428, 1024, 1024)
        val ones = Array.fill(1024 * 1024)(1)
        val raster = Raster(ones, rasterExtent)
        val countOp = geotrellis.vector.op.data.RasterizePolygon(raster, p)
        //val countOp = TiledPolygonalZonalCount(p, Raster.empty(rasterExtent), Map())
        server.run(countOp)
        //          ((-70.29017892250002 53.43749954099997, -70.29017892250002 35.15178531379998, -88.57589314970001 35.15178531379998, -88.57589314970001 53.43749954099997, -70.29017892250002 53.43749954099997))

    }
  }
}
