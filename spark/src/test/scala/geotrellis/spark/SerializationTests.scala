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

package geotrellis.spark

import geotrellis.proj4._
import geotrellis.raster.io.geotiff._
import geotrellis.spark.testkit._
import geotrellis.vector._

import org.apache.hadoop.fs.Path
import org.scalatest._

class SerializationTests extends FunSuite with Matchers {
  test("Serializing CRS's") {
    val crs = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")
    assert(crs == LatLng)

    {
      val t = Transform(crs, LatLng)
      val expected = (141.7066666666667, -17.946666666666676)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

    {
      val (crs1, crs2) = (crs.serializeAndDeserialize, LatLng.serializeAndDeserialize)
      assert(crs1 == crs2)
      val t = Transform(crs1, crs2)
      val expected = (141.7066666666667, -17.946666666666676)
      val actual = t(expected._1, expected._2)
      actual should be (expected)
    }

    {
      val t = Transform(LatLng, crs)
      val expected = (141.7154166666667,-17.52875000000001)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

    {
      val t = Transform(LatLng, crs.serializeAndDeserialize)
      val expected = (141.7154166666667,-17.52875000000001)
      val actual = t(expected._1, expected._2)
      assert(actual == expected)
    }

  }
}
