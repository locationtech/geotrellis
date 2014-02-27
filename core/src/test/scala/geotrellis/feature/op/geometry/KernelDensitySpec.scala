package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.testutil._

import org.scalatest.FunSuite

class KernelDensitySpec extends FunSuite with TestServer {
  test("kernel density") {
    val rasterExtent = RasterExtent(Extent(0,0,5,5),1,1,5,5)
    val n = NODATA
    val arr = Array(2,2,1,n,n,
                    2,3,2,1,n,
                    1,2,2,1,n,
                    n,1,1,2,1,
                    n,n,n,1,1)
    val r = Raster(arr,rasterExtent)

    val kernel = Raster(Array(1,1,1,
                              1,1,1,
                              1,1,1),RasterExtent(Extent(0,0,3,3),1,1,3,3))

    val points = Seq(Point(0,4.5,1),Point(1,3.5,1),Point(2,2.5,1),Point(4,0.5,1))
    val source = 
      VectorToRaster.kernelDensity(points, kernel, rasterExtent)

    assertEqual(source.get, r)
  }
}
