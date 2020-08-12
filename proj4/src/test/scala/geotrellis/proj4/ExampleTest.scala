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

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/**
 * Test which serves as an example of using Proj4J.
 *
 * @author mbdavis
 *
 */
class ExampleTest extends AnyFunSuite with Matchers {
  def isInTolerance(p: ProjCoordinate, x: Double, y: Double, tolerance: Double) = {
    /*
     * Compare result to expected, for test purposes
     */
    val dx = math.abs(p.x - x)
    val dy = math.abs(p.y - y)
    dx <= tolerance && dy <= tolerance
  }

  def checkTransform(csName: String, lon: Double, lat: Double, expectedX: Double, expectedY: Double, tolerance: Double) = {
    val ctFactory = new CoordinateTransformFactory()
    val csFactory = new CRSFactory()
    /*
     * Create {@link CoordinateReferenceSystem} & CoordinateTransformation.
     * Normally this would be carried out once and reused for all transformations
     */
    val crs = csFactory.createFromName(csName)

    val WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees"
    val WGS84 = csFactory.createFromParameters("WGS84",WGS84_PARAM)

    val trans = ctFactory.createTransform(WGS84, crs)

    /*
     * Create input and output points.
     * These can be constructed once per thread and reused.
     */
    val p = new ProjCoordinate()
    val p2 = new ProjCoordinate()
    p.x = lon
    p.y = lat

    /*
     * Transform point
     */
    trans.transform(p, p2)

    isInTolerance(p2, expectedX, expectedY, tolerance)
  }

  test("TransformToGeographic") {
    checkTransform("EPSG:2227", -121.3128278, 37.95657778, 6327319.23 , 2171792.15, 0.01 ) should be (true)
  }

  test("ExplicitTransform") {
    val csName1 = "EPSG:32636"
    val csName2 = "EPSG:4326"

    val ctFactory = new CoordinateTransformFactory()
    val csFactory = new CRSFactory()
    /*
     * Create {@link CoordinateReferenceSystem} & CoordinateTransformation.
     * Normally this would be carried out once and reused for all transformations
     */
    val crs1 = csFactory.createFromName(csName1)
    val crs2 = csFactory.createFromName(csName2)

    val trans = ctFactory.createTransform(crs1, crs2)

    /*
     * Create input and output points.
     * These can be constructed once per thread and reused.
     */
    val p1 = new ProjCoordinate()
    val p2 = new ProjCoordinate()
    p1.x = 500000
    p1.y = 4649776.22482

    /*
     * Transform point
     */
    trans.transform(p1, p2)

    isInTolerance(p2, 33, 42, 0.000001) should be (true)
  }
}
