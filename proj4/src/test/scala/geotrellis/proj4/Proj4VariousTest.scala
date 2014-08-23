package geotrellis.proj4

import org.osgeo.proj4j._

import org.scalatest._

/**
 * Tests from the PROJ4 testvarious file.
 * 
 * @author Martin Davis (port by Rob emanuele)
 *
 */
class Proj4VariousTest extends FunSuite with BaseCoordinateTransformTest
{ 
  test("RawEllipse") {
    checkTransform(
        "+proj=latlong +ellps=clrk66", p("79d58'00.000W 37d02'00.000N"), 
        "+proj=latlong +ellps=bessel", p("79d58'W 37d2'N"), 0.01 )
    checkTransform(
        "+proj=latlong +ellps=clrk66", p("79d58'00.000\"W 36d58'00.000\"N"), 
        "+proj=latlong +ellps=bessel", p("79d58'W 36d58'N"), 0.01 )
  }

  test("NAD27toRawEllipse") {
    checkTransform(
        "+proj=latlong +datum=NAD27", p("79d00'00.000\"W 35d00'00.000\"N"), 
        "+proj=latlong +ellps=bessel", p("79dW 35dN"), 0.01 )
  }
  
  test("3ParamApproxSameEllipsoid") {
    checkTransform(
        "+proj=latlong +ellps=bessel +towgs84=5,0,0", p("0d00'00.000W 0d00'00.000N"), 
        "+proj=latlong +ellps=bessel +towgs84=1,0,0", p("0dE  0dN 4.000"), 1e-5 )
    checkTransform(
        "+proj=latlong +ellps=bessel +towgs84=5,0,0", p("79d00'00.000W 45d00'00.000N 0.0"), 
        "+proj=latlong +ellps=bessel +towgs84=1,0,0", p("78d59'59.821W  44d59'59.983N 0.540"), 1e-5 )
  }
  test("3ParamToRawSameEllipsoid") {
    checkTransform(
        "+proj=latlong +ellps=bessel +towgs84=5,0,0", p("0d00'00.000W 0d00'00.000N"), 
        "+proj=latlong +ellps=bessel", p("0dE  0dN 4.000"), 1e-5 )
  }
  
  ignore("FAIL_test3ParamToRawSameEllipsoid2") {
    // fails - not sure why, possibly missing towgs not handled in same way as PROJ4?
    checkTransform(
        "+proj=latlong +ellps=bessel +towgs84=5,0,0", p("79d00'00.000W 45d00'00.000N 0.0"), 
        "+proj=latlong +ellps=bessel", p("79dW  45dN 0.000"), 1e-5 )
  }
  
  test("Stere") {
    checkTransform(
        "+proj=latlong +datum=WGS84", p("105 40"), 
        "+proj=stere +lat_0=90 +lon_0=0 +lat_ts=70 +datum=WGS84", p("5577808.93 1494569.40 0.00"), 1e-2 )
  }

  test("StereWithout_lat_ts") {
    checkTransform(
        "+proj=latlong +datum=WGS84", p("20 45"), 
        "+proj=stere +lat_0=40 +lon_0=10  +datum=WGS84", p("789468.08 602385.33 0.00"), 1e-2 )
  }

  test("STS") {
    checkTransform(
        "+proj=latlong +datum=WGS84", p("4.897000 52.371000"), 
        "+proj=kav5 +ellps=WGS84 +units=m", p("383646.09  5997047.89"), 1e-2 )
    checkTransform(
        "+proj=kav5 +ellps=WGS84 +units=m", p("383646.088858 5997047.888175"),
        "+proj=latlong +datum=WGS84", p("4d53'49.2E  52d22'15.6N"), 
        1e-5 )
  }

  // disabled - gamma param not implemented
  ignore("XXX_testRSOBorneo") {
    checkTransform(
        "+proj=latlong +a=6377298.556 +rf=300.8017", p("116d2'11.12630 5d54'19.90183"), 
        "+proj=omerc +a=6377298.556 +rf=300.8017 +lat_0=4 +lonc=115 +alpha=53d18'56.9537 +gamma=53d7'48.3685  +k_0=0.99984 +x_0=590476.87 +y_0=442857.65", 
        p("704570.40  653979.68"), 1e-2 )
  }

  ignore("FAIL_testPconic") {
    checkTransform(
        "+proj=latlong +datum=WGS84", p("-70.4 -23.65"), 
        "+proj=pconic  +units=m +lat_1=20n +lat_2=60n +lon_0=60W +datum=WGS84", p("-2240096.40  -6940342.15"),
        1e-2 )
    // Known failure case
    checkTransform(
        "+proj=pconic  +units=m +lat_1=20n +lat_2=60n +lon_0=60W +datum=WGS84", p("-2240096.40  -6940342.15"),
        "+proj=latlong +datum=WGS84", p("-70.4 -23.65"), 
        1e-2 )
  }
}
  
