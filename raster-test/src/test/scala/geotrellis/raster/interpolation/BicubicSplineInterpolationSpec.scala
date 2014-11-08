package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

/**
  * Since cubic spline interpolation inherits from cubic interpolation it
  * is only needed to test the actual math, the rest is tested in the bicubic
  * interpolation spec, the cubic interpolation spec and the bilinear
  * interpolation spec.
  *
  * It is also known that the bicubic interpolation resolves, if the cube
  * is D * D points, first D rows then the D values of each interpolation
  * result, and interpolates them. So only one dimensional interpolation
  * is needed to be tested.
  *
  * The bicubic spline interpolation class uses a helper class,
  * which is the only class tested here.
  */
class BicubicSplineInterpolationSpec
    extends FunSuite with Matchers {

  val E = 1e-2

  val ip = new CubicSplineInterpolation()

  /*
   * These test cases are derived from using a MATLAB code snippet called
   * cakima.m found here:
   * http://www.mathworks.com/matlabcentral/fileexchange/36800-interpolation-utilities/content//cakima.m
   *
   * The only difference is that the MATLAB function uses a 1.XX prefix and we a
   * 0.XX prefix for the x parameter.
   */
  test("one dimensional cubic akima splines should work as expected") {
    ip.interpolate(Array(0, 0, 0, 0, 0, 0), 0) should be (0.0 +- E)
    ip.interpolate(Array(0, 1, 2, 3, 4, 5), 0.5) should be (2.5 +- E)
    ip.interpolate(Array(6, 8, 3, 4, 9, 5), 0.5) should be (3.02727 +- E)
    ip.interpolate(Array(15, 32, 10, 54, 22, 11), 0.33) should be (22.2643 +- E)
    ip.interpolate(Array(20, 40, 60, 80, 60, 20), 0.74) should be (74.8 +- E)
    ip.interpolate(Array(-12, 23, -5, -1, 0, 32), 0.45) should be (-3.2283 +- E)
    ip.interpolate(Array(65.5, 15.5, 45.1, 22.2, 11, 5), 0.89) should be (23.8639 +- E)
    ip.interpolate(Array(1, 2, 4, 8, 16, 32), 0.61) should be (6.1755 +- E)
    ip.interpolate(Array(-10, 15, 25, 25, 64, 128), 0.22) should be (25.546 +- E)
    ip.interpolate(Array(25, 20, 15, 10, 5, 0), 0.333) should be (13.3350 +- E)
  }

}
