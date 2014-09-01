package geotrellis.proj4

import geotrellis.proj4.units.Angle

import org.scalatest._

/**
  * Tests correctness and accuracy of Coordinate System transformations.
  * 
  * @author Martin Davis (port by Rob Emanuele)
  */
trait BaseCoordinateTransformTest extends Matchers
{
  // ~= 1 / (2Pi * Earth radius)
  // in code: 1.0 / (2.0 * Math.PI * 6378137.0)
  final val APPROX_METRE_IN_DEGREES = 2.0e-8
  
  final val debug = true
  
  val tester = new CoordinateTransformTester(true)
  
  def p(pstr: String): ProjCoordinate = {
    val pord = pstr.split("\\s+")
    val p0 = Angle.parse(pord(0))
    val p1 = Angle.parse(pord(1))
    if (pord.length > 2) {
      val p2 = pord(2).toDouble
      new ProjCoordinate(p0, p1, p2)
    } else {
      new ProjCoordinate(p0, p1)
    }
  }

  def checkTransformFromWGS84(code: String, lon: Double, lat: Double, x: Double, y: Double) = {
    tester.checkTransformFromWGS84(code, lon, lat, x, y, 0.0001) should be (true)
  }

  def checkTransformFromWGS84(code: String, lon: Double, lat: Double, x: Double, y: Double, tolerance: Double) = {
    tester.checkTransformFromWGS84(code, lon, lat, x, y, tolerance) should be (true)
  }

  def checkTransformToWGS84(code: String, x: Double, y: Double, lon: Double, lat: Double, tolerance: Double) = {
    tester.checkTransformToWGS84(code, x, y, lon, lat, tolerance) should be (true)
  }

  def checkTransformFromGeo(code: String, lon: Double, lat: Double, x: Double, y: Double) = {
    tester.checkTransformFromGeo(code, lon, lat, x, y, 0.0001) should be (true)
  }

  def checkTransformFromGeo(code: String, lon: Double, lat: Double, x: Double, y: Double, tolerance: Double) = {
    tester.checkTransformFromGeo(code, lon, lat, x, y, tolerance) should be (true)
  }

  def checkTransformToGeo(code: String, x: Double, y: Double, lon: Double, lat: Double, tolerance: Double) = {
    tester.checkTransformToGeo(code, x, y, lon, lat, tolerance) should be (true)
  }

  def checkTransformFromAndToGeo(code: String, lon: Double, lat: Double, x: Double, y: Double, tolProj: Double, tolGeo: Double) = {
    tester.checkTransformFromGeo(code, lon, lat, x, y, tolProj) should be (true)
    tester.checkTransformToGeo(code, x, y, lon, lat, tolGeo) should be (true)
  }

  def checkTransform(
    cs1: String, x1: Double, y1: Double,
    cs2: String, x2: Double, y2: Double,
    tolerance: Double) =  {
    tester.checkTransform(cs1, x1, y1, cs2, x2, y2, tolerance) should be (true)
  }

  def checkTransform(
    cs1: String, p1: ProjCoordinate,
    cs2: String, p2: ProjCoordinate,
    tolerance: Double) =  {
    tester.checkTransform(cs1, p1, cs2, p2, tolerance) should be (true)
  }

  def checkTransformAndInverse(
    cs1:String, x1: Double, y1: Double,
    cs2: String, x2: Double, y2: Double,
    tolerance: Double,
    inverseTolerance: Double) = {
    tester.checkTransform(cs1, x1, y1, cs2, x2, y2, tolerance, inverseTolerance, true) should be (true)
  }
  
}
