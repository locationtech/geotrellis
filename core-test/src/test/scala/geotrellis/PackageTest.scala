/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers._

class PackageTest extends FunSuite with ShouldMatchers {
  test("constants") {
    assert(NODATA === Int.MinValue)
  }

  test("isNoData - Int") {
    val v = Int.MinValue + 1
    val y = 123

    isNoData(v - 1) should be (true)
    isNoData(y) should be (false)
  }

  test("isData - Int") {
    val v = Int.MinValue + 1
    val y = 123

    isData(v-1) should be (false)
    isData(y) should be (true)
  }

  test("isNoData - Double") {
    val x = 2.1 + Double.NaN
    val y = 1.3

    isNoData(x*3 - 1.0) should be (true)
    isNoData(y) should be (false)
  }

  test("isData - Double") {
    val x = 2.1 + Double.NaN
    val y = 1.3

    isData(x*3 - 1.0) should be (false)
    isData(y) should be (true)
  }
}
