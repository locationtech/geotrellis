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

class RepeatedTransformTest extends AnyFunSuite with Matchers {
  test("RepeatedTransform") {
    val crsFactory = new CRSFactory()

    val src = crsFactory.createFromName("epsg:4326")
    val dest = crsFactory.createFromName("epsg:27700")

    val ctf = new CoordinateTransformFactory()
    val transform = ctf.createTransform(src, dest)

    val srcPt = new ProjCoordinate(0.899167, 51.357216)
    val destPt = new ProjCoordinate()

    transform.transform(srcPt, destPt)
    // System.out.println(srcPt + " ==> " + destPt)

    // do it again
    val destPt2 = new ProjCoordinate()
    transform.transform(srcPt, destPt2)
    // System.out.println(srcPt + " ==> " + destPt2)

    destPt should be (destPt2)
  }
}
