/*
 * Copyright 2017 Astraea, Inc.
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

package geotrellis.raster.io.json

import geotrellis.raster.CellSize

import io.circe.parser._
import cats.syntax.either._

import org.scalatest.Assertions
import org.scalatest.funspec.AnyFunSpec


class CellSizeJsonSpec extends AnyFunSpec with Assertions {

  describe("CellSize should not be truncated when reading") {
    val json =
      """
        |{
        |  "width": 463.3127165274989,
        |  "height": 463.3127165274989
        |}
      """.stripMargin

    val cs = decode[CellSize](json).valueOr(throw _)

    assert(Math.abs(cs.height - 463.3127165274989) < .000000001)
    assert(Math.abs(cs.width - 463.3127165274989) < .000000001)
  }
}
