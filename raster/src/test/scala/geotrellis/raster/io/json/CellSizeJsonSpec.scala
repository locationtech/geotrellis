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
import geotrellis.raster.io._
import org.scalatest.{Assertions, FunSpec}
import spray.json._

class CellSizeJsonSpec extends FunSpec with Assertions {

  describe("CellSize should not be truncated when reading") {
    val json =
      """
        |{
        |   "cellSize": {
        |     "width": 463.3127165274989,
        |     "height": 463.3127165274989
        |   }
        |}
      """.stripMargin
    json.parseJson match {
      case JsObject(fields) => {
        val cs = fields.get("cellSize").map(_.convertTo[CellSize])
        assert(cs.isDefined)
        assert(Math.abs(cs.get.height - 463.3127165274989) < .000000001)
        assert(Math.abs(cs.get.width - 463.3127165274989) < .000000001)
      }
      case _ => fail("Could not parse cellSize")
    }
  }
}
