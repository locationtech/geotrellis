package geotrellis.proj4

import org.scalatest._

class RepeatedTransformTest extends FunSuite with Matchers {
  test("RepeatedTransform") {
    val crsFactory = new CRSFactory()

    val src = crsFactory.createFromName("epsg:4326")
    val dest = crsFactory.createFromName("epsg:27700")

    val ctf = new CoordinateTransformFactory()
    val transform = ctf.createTransform(src, dest)
    
    val srcPt = new ProjCoordinate(0.899167, 51.357216)
   
    val destPt = transform.transform(srcPt)
    System.out.println(srcPt + " ==> " + destPt)
    
    // do it again
    val destPt2 = transform.transform(srcPt)
    System.out.println(srcPt + " ==> " + destPt2)

    destPt should be (destPt2)
  }
}
