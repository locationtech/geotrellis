package geotrellis.proj4.proj

import org.osgeo.proj4j.proj._

import org.scalatest._

import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem

/**
  * Tests accuracy and correctness of projecting and reprojecting a grid of geographic coordinates.
  * 
  * @author Martin Davis (port by Rob Emanuele)
  *
  */
class ProjectionGridTest extends FunSuite with Matchers {
  val TOLERANCE = 0.00001
  
  test("Albers") {
    runEPSG(3005)
  }
  
  test("StatePlane") {
    // State-plane EPSG defs
    runEPSG(2759, 2930)
  }
  test("StatePlaneND") {
    runEPSG(2265)
  }
  
  def runEPSG(codeStart: Int, codeEnd: Int): Unit = {
    for(i <- codeStart to codeEnd) {
      runEPSG(i)
    }
  }
  
  def runEPSG(code: Int): Unit = {
    run("epsg:" + code)
  }
  
  def run(code: String): Unit = {
    val csFactory = new CRSFactory()
    val cs = csFactory.createFromName(code)
    if (cs != null) {
      val tripper = new ProjectionGridRoundTripper(cs)

      val (isOK, (xmin, ymin, xmax, ymax)) = tripper.runGrid(TOLERANCE)
      
      System.out.println(code + " - " + cs.getParameterString())
      System.out.println(s" - extent: [ $xmin, $ymin, $xmax, $ymax ]")
      System.out.println(s" - tol: $TOLERANCE")
      System.out.println(s" - # pts run = ${tripper.transformCount}")
      
      isOK should be (true)
    }
  }
}
