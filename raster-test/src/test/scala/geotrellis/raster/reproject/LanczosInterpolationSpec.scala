package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

/**
  * Since lanczos interpolation inherits from cubic interpolation it
  * is only needed to test the actual math, the rest is tested in the cubic
  * interpolation spec and the bilinear interpolation spec.
  *
  * It is also known that the bicubic interpolation resolves, if the cube
  * is D * D points, first D rows then the D values of each interpolation
  * result, and interpolates them.
  *
  * The lanczos interpolation is not divided into unidimensional interpolations
  * when doing bidiminensional interpolations. Therefore the testing revolves
  * around testing different matrices.
  *
  * The LanczosInterpolation class uses a helper class, which is the only class
  * being tested here.
  *
  * I haven't been able to find a single good source for lanczos
  * interpolation in two dimensions.
  *
  * These values are calculated by hand by Johan Stenberg, and should match the
  * GeoTrellis lanczos lnterpolation implementation.
  */
class LanczosInterpolationSpec extends FunSpec with Matchers {

  val Eps = 1e-6

  describe("unidimensional lanczos interpolation should work correctly") {

    it("should work correctly") {
      val interp = new LanczosInterpolator

      val inputAndAnswers = List[(Double, Double)](
        (0, 1),
        (3 + Eps, 0),
        (-3 - Eps, 0),
        (0.5, 0.607927),
        (0.25, 0.890067),
        (0.75, 0.27019),
        (0.1, 0.981835),
        (0.9, 0.0938159),
        (0.35, 0.792311),
        (0.65, 0.403411),
        (0.01, 0.999817),
        (0.99, 0.00838499)
      )

      for ((input, answer) <- inputAndAnswers)
        interp.lanczos(input) should be (answer +- Eps)
    }

  }

  /*
   * If we know that the unidimensional lanczos interpolation actually works,
   * we only need to verify that the twodimensional lanczos interpolation
   * accumulates the unidimensional results correctly.
   */
  describe("bidimensional lanczos interpolation should work correctly") {

    it("should iterate through all values correctly") {
      val array = Array(
        Array(1, 2, 3, 4, 5, 6.0),
        Array(7, 8, 9, 10, 11, 12.0),
        Array(13, 14, 15, 16, 17, 18.0),
        Array(19, 20, 21, 22, 23, 24.0),
        Array(25, 26, 27, 28, 29, 30.0),
        Array(31, 32, 33, 34, 35, 36.0)
      )

      val interp = new LanczosInterpolator {
        override def lanczos(v: Double) = 1
      }

      interp.interpolate(array, 0, 0) should be (666)
    }

    it("should iterate through all values correctly and let x and y contribute equally") {
      val array = Array(
        Array(1, 2, 3, 4, 5, 6.0),
        Array(7, 8, 9, 10, 11, 12.0),
        Array(13, 14, 15, 16, 17, 18.0),
        Array(19, 20, 21, 22, 23, 24.0),
        Array(25, 26, 27, 28, 29, 30.0),
        Array(31, 32, 33, 34, 35, 36.0)
      )

      val interp = new LanczosInterpolator {
        override def lanczos(v: Double) = if (v % 1 != 0) 1 else 0
      }

      interp.interpolate(array, 0, 0.5) should be (0)
      interp.interpolate(array, 0.5, 0) should be (0)
      interp.interpolate(array, 0.5, 0.5) should be (666)
    }

  }

}
