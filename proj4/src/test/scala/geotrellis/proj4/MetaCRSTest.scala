/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.proj4

import org.locationtech.proj4j._
import org.locationtech.proj4j.util._

import java.io.File

import org.scalatest.matchers.{ BeMatcher, MatchResult }
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Runs MetaCRS test files.
 *
 * @author mbdavis (port by Rob Emanuele)
 */
class MetaCRSTest extends AnyFunSuite with Matchers {
  val crsFactory = new CRSFactory

  test("MetaCRSExample") {
    val file = new File("src/test/resources/TestData.csv")
    val tests = MetaCRSTestFileReader.readTests(file)
    for (test <- tests) {
      test should be(passing)
    }
  }

  test("PROJ4_SPCS") {
    val file = new File("src/test/resources/PROJ4_SPCS_EPSG_nad83.csv")
    val tests = MetaCRSTestFileReader.readTests(file)
    for (test <- tests) {
      test should be(passing)
    }
  }

  // TODO: update this test, started failing with switch from EPSG Database 8.6 to 9.2
  ignore("PROJ4_Empirical") {
    val file = new File("src/test/resources/proj4-epsg.csv")
    val tests = MetaCRSTestFileReader.readTests(file)
    for (test <- tests) {
      test.testMethod match {
        case "passing" => test should be(passing)
        case "failing" => test should not(be(passing))
        case "error" => intercept[org.locationtech.proj4j.Proj4jException] { test.execute(crsFactory) }
      }
    }
  }
}

object passing extends BeMatcher[MetaCRSTestCase] {
  val crsFactory = new CRSFactory
  def apply(left: MetaCRSTestCase) = {
    import left._
    val (success, x, y) = left.execute(crsFactory)
    MatchResult(
      success,
      f"$srcCrsAuth:$srcCrs→$tgtCrsAuth:$tgtCrs ($srcOrd1, $srcOrd2, $srcOrd3) → ($tgtOrd1, $tgtOrd2, $tgtOrd3); got ($x, $y)",
      f"$srcCrsAuth:$srcCrs→$tgtCrsAuth:$tgtCrs in tolerance")
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

  val srcPt = new ProjCoordinate()
  val resultPt = new ProjCoordinate()

  def sourceCrsName = csName(srcCrsAuth, srcCrs)
  def targetCrsName = csName(tgtCrsAuth, tgtCrs)

  def sourceCoordinate = new ProjCoordinate(srcOrd1, srcOrd2, srcOrd3)

  def targetCoordinate = new ProjCoordinate(tgtOrd1, tgtOrd2, tgtOrd3)

  def resultCoordinate = new ProjCoordinate(resultPt.x, resultPt.y)

  // public void setCache(CRSCache crsCache)
  // {
  //   this.crsCache = crsCache
  // }

  def execute(csFactory: CRSFactory): (Boolean, Double, Double) = {
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

  def executeTransform(srcCS: CoordinateReferenceSystem, tgtCS: CoordinateReferenceSystem): (Boolean, Double, Double) = {
    srcPt.x = srcOrd1
    srcPt.y = srcOrd2
    // Testing: flip axis order to test SS sample file
    //srcPt.x = srcOrd2
    //srcPt.y = srcOrd1

    val trans = new BasicCoordinateTransform(srcCS, tgtCS)

    trans.transform(srcPt, resultPt)

    val dx = math.abs(resultPt.x - tgtOrd1)
    val dy = math.abs(resultPt.y - tgtOrd2)
    // println(srcPt, resultPt, (tgtOrd1, tgtOrd2), (dx, dy), (tolOrd1, tolOrd2))

    (dx <= tolOrd1 && dy <= tolOrd2, resultPt.x, resultPt.y)
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
        + srcCS.getParameterString())
      System.out.println("Tgt CRS: ("
        + tgtCrsAuth + ":" + tgtCrs + ") "
        + tgtCS.getParameterString())
    }
  }
}
