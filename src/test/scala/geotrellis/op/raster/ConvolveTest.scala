package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op.VerticalFlip

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConvolveTest extends FunSuite {
  val server = TestServer("src/test/resources/catalog.json")

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

    val r1 = new Raster(data1, re1)
    val r2 = new Raster(data2, re2)

    val op = Convolve(r1, r2)
    val r3 = server.run(op)

    val r3d = r3.data.asArray.get.asInstanceOf[IntArrayRasterData].array
    assert(r3d === out)
  }
  
  test("gaussian") {
    // Create and sample a 5x5 guassian
    val op = Gaussian(5,5.0,2.0,100.0)
    val r1 = server.run(op)
    val r1d = r1.data.asArray.get

    // (3,1) => (1,1) => r = sqrt(1*1 + 1*1) = sqrt(2)
    // 100*exp(-(sqrt(2)^2)/(2*(2.0^2))) = 77.88 = 77
    assert(r1d.get(3,1) === 77)

    // Should also be symmetric
    assert(r1d.get(3,3) === 77)
    assert(r1d.get(3,1) === 77)
    assert(r1d.get(1,3) === 77)
    assert(r1d.get(1,1) === 77)

    // Make sure amp and sigma do stuff
    val op2 = Gaussian(5,5.0,4.0,50.0)
    val r2 = server.run(op2)
    val r2d = r2.data.asArray.get

    // (3,1) => (1,1) => r = sqrt(1*1 + 1*1) = sqrt(2)
    // 50*exp(-(sqrt(2)^2)/(2*(4.0^2))) = 46.97 = 46
    assert(r2d.get(3,1) === 46)
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

  test("even kernel") {
    doit(Array(1,2,3,4,
               5,6,7,8,
               9,10,11,12,
               13,14,15,16),
         Array(1,2,
               3,4),
         Array(26, 36, 46, 32, 
               66, 76, 86, 56, 
               106, 116, 126, 80, 
               94, 101, 108, 64))
  }
}
