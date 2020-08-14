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

package geotrellis.proj4.proj

import org.locationtech.proj4j.CRSFactory

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
  * Tests accuracy and correctness of projecting and reprojecting a grid of geographic coordinates.
  *
  * @author Martin Davis (port by Rob Emanuele)
  *
  */
class ProjectionGridTest extends AnyFunSuite with Matchers {
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

      // System.out.println(code + " - " + cs.getParameterString())
      // System.out.println(s" - extent: [ $xmin, $ymin, $xmax, $ymax ]")
      // System.out.println(s" - tol: $TOLERANCE")
      // System.out.println(s" - # pts run = ${tripper.transformCount}")

      isOK should be (true)
    }
  }
}
