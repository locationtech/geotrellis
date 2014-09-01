package geotrellis.proj4

import geotrellis.proj4.util._

import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.List

import org.scalatest._

import scala.collection.JavaConversions._

/**
 * Runs MetaCRS test files.
 * 
 * @author mbdavis (port by Rob Emanuele)
 */
class MetaCRSTest extends FunSuite with Matchers {
  val crsFactory = new CRSFactory

  test("MetaCRSExample") {
    val file = new File("proj4/src/test/resources/TestData.csv")
    val tests = MetaCRSTestFileReader.readTests(file)
    for (test <- tests) {
      test.execute(crsFactory)
    }
  }

  test("PROJ4_SPCS") {
    val file = new File("proj4/src/test/resources/PROJ4_SPCS_EPSG_nad83.csv")
    val tests = MetaCRSTestFileReader.readTests(file)
    for (test <- tests) {
      test.execute(crsFactory)
    }
  }
}

case class MetaCRSTestCase(
  testName: String,
  testMethod: String,
  srcCrsAuth: String,
  srcCrs: String,
  tgtCrsAuth: String,
  tgtCrs: String,
  srcOrd1: Double,
  srcOrd2: Double,
  srcOrd3: Double,
  tgtOrd1: Double,
  tgtOrd2: Double,
  tgtOrd3: Double,
  tolOrd1: Double,
  tolOrd2: Double,
  tolOrd3: Double,
  using: String,
  dataSource: String,
  dataCmnts: String,
  maintenanceCmnts: String
) {
  val verbose = true

  var srcPt = ProjCoordinate()
  var resultPt = ProjCoordinate()

  def sourceCrsName = csName(srcCrsAuth, srcCrs) 
  def targetCrsName = csName(tgtCrsAuth, tgtCrs)
  
  def sourceCoordinate = new ProjCoordinate(srcOrd1, srcOrd2, srcOrd3)
  
  def targetCoordinate = new ProjCoordinate(tgtOrd1, tgtOrd2, tgtOrd3)
  
  def resultCoordinate = new ProjCoordinate(resultPt.x, resultPt.y)
  
  // public void setCache(CRSCache crsCache)
  // {
  //   this.crsCache = crsCache
  // }
  
  def execute(csFactory: CRSFactory): Boolean = {
    val srcCS = createCS(csFactory, srcCrsAuth, srcCrs)
    val tgtCS = createCS(csFactory, tgtCrsAuth, tgtCrs)
    executeTransform(srcCS, tgtCS)
  }
  
  def csName(auth: String, code: String) =
    auth + ":" + code
  
  def createCS(csFactory: CRSFactory, auth: String, code: String) = {
    val name = csName(auth, code)
    
    csFactory.createFromName(name)
  }
  
  def executeTransform(srcCS: CoordinateReferenceSystem, tgtCS: CoordinateReferenceSystem): Boolean = {
    srcPt = ProjCoordinate(srcOrd1, srcOrd2)
    
    val trans = new BasicCoordinateTransform(srcCS, tgtCS)

    resultPt =
      trans.transform(srcPt)
    
    val dx = math.abs(resultPt.x - tgtOrd1)
    val dy = math.abs(resultPt.y - tgtOrd2)
    
    dx <= tolOrd1 && dy <= tolOrd2
  }

  def print(isInTol: Boolean, srcCS: CoordinateReferenceSystem, tgtCS: CoordinateReferenceSystem) = {
    System.out.println(testName)
    System.out.println(ProjectionUtil.toString(srcPt)
      + " -> " + ProjectionUtil.toString(resultPt)
      + " ( expected: " + tgtOrd1 + ", " + tgtOrd2 + " )"
    )

    
    if (!isInTol) {
      System.out.println("FAIL")
      System.out.println("Src CRS: ("
        + srcCrsAuth + ":" + srcCrs + ") "
        + srcCS.parameterString)
      System.out.println("Tgt CRS: ("
        + tgtCrsAuth + ":" + tgtCrs + ") "
        + tgtCS.parameterString)
    }
  }
}

