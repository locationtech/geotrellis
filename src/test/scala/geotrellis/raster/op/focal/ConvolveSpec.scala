package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConvolveSpec extends FunSuite with TestServer {
  def doit(in1: Array[Int], in2: Array[Int], out: Array[Int]) = {
    val size1 = math.sqrt(in1.length).toInt    
    assert(size1*size1 === in1.length)

    val size2 = math.sqrt(in2.length).toInt
    assert(size2*size2 === in2.length)

    val e1 = Extent(0,0,10*size1,10*size1)
    val e2 = Extent(0,0,10*size2,10*size2)
    val re1 = RasterExtent(e1, 10,10,size1,size1)
    val re2 = RasterExtent(e2, 10,10,size2,size2)

    val data1 = IntArrayRasterData(in1, size1, size1)
    val data2 = IntArrayRasterData(in2, size2, size2)

    val r1 = Raster(data1, re1)
    val r2 = Raster(data2, re2)

    val op = Convolve(r1, r2)
    val r3 = server.run(op)

    val r3d = r3.toArray
    assert(r3d === out)
  }
  
  test("gaussian") {
    // Create and sample a 5x5 guassian
    val op = CreateGaussianRaster(5,5.0,2.0,100.0)
    val r1 = server.run(op)

    // (3,1) => (1,1) => r = sqrt(1*1 + 1*1) = sqrt(2)
    // 100*exp(-(sqrt(2)^2)/(2*(2.0^2))) = 77.88 = 77
    assert(r1.get(3,1) === 77)

    // Should also be symmetric
    assert(r1.get(3,3) === 77)
    assert(r1.get(3,1) === 77)
    assert(r1.get(1,3) === 77)
    assert(r1.get(1,1) === 77)

    // Make sure amp and sigma do stuff
    val op2 = CreateGaussianRaster(5,5.0,4.0,50.0)
    val r2 = server.run(op2)

    // (3,1) => (1,1) => r = sqrt(1*1 + 1*1) = sqrt(2)
    // 50*exp(-(sqrt(2)^2)/(2*(4.0^2))) = 46.97 = 46
    assert(r2.get(3,1) === 46)
  }
 

  test("simple convolve") {
    /* http://www.songho.ca/dsp/convolution/convolution2d_example.html */
    doit(Array(1,2,3,
               4,5,6,
               7,8,9),
         Array(-1,-2,-1,
               0,0,0,
               1,2,1),
         Array(-13,-20,-17,
               -18,-24,-18,
               13,20,17))
  }

  test("impulse") {
    // Impulse test
    val a = Array(1,2,3,4,
               5,6,7,8,
               9,10,11,12,
               13,14,15,16)
    doit(a,
         Array(0,0,0,
               0,1,0,
               0,0,0),
         a)
  }
}
