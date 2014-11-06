package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

/**
  * Since cubic convolution interpolation inherits from cubic interpolation it
  * is only needed to test the actual math, the rest is tested in the bicubic
  * interpolation spec, the cubic interpolation spec and the bilinear
  * interpolation spec.
  *
  * It is also known that the bicubic interpolation resolves, if the cube
  * is D * D points, first D rows then the D values of each interpolation
  * result, and interpolates them. So only one dimensional interpolation
  * is needed to be tested.
  *
  * The bicubic convolution interpolation class uses a helper class,
  * which is the only class tested here.
  */
class BicubicConvolutionInterpolationSpec
    extends FunSuite with Matchers {

  val E = 1e-4

  val ip = new CubicConvolutionInterpolation()

  /*
   * These test cases are derived from using a MATLAB code snippet called
   * cubiconv.m found here:
   * http://www.mathworks.com/matlabcentral/fileexchange/36800-interpolation-utilities/content/cubiconv.m
   *
   * The only difference is that the MATLAB function uses a 1.XX prefix and we a
   * 0.XX prefix for the x parameter.
   */
  test("one dimensional cubic convolution should work as expected") {
    ip.interpolate(Array(0, 0, 0, 0), 0) should be (0.0 +- E)
    ip.interpolate(Array(0, 1, 2, 3), 0.5) should be (1.5 +- E)
    ip.interpolate(Array(6, 8, 2, 7), 0.5) should be (4.8125 +- E)
    ip.interpolate(Array(-4, 65.5, -13, -5), 0.33) should be (47.4015 +- E)
    ip.interpolate(Array(100, 0, -100, 137), 0.89) should be (-103.6816 +- E)
    ip.interpolate(Array(0.65, 0.91, 12, 43), 0.52) should be (4.736 +- E)
    ip.interpolate(Array(1e2, 2e2, 1e2, 0), 0.17) should be (194.7113 +- E)
    ip.interpolate(Array(-20, -20, 1e2, 0), 0.85) should be (92.7738 +- E)
    ip.interpolate(Array(5, 3, 8, 12), 0.61) should be (5.7978 +- E)
    ip.interpolate(Array(-1, -10, -3, -6), 0.23) should be (-9.2773 +- E)
  }

}
