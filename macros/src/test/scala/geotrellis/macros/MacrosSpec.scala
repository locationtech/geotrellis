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

package geotrellis.macros

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MacrosSpec extends AnyFunSpec with Matchers {

  it("NoDataMacros: sentinel detection, and isData as its negation") {
    CallSites.isNoData(Int.MinValue) should be (true)
    CallSites.isNoData(0) should be (false)
    CallSites.isNoData(Double.NaN) should be (true)
    CallSites.isData(Int.MinValue) should be (false)
    CallSites.isData(1) should be (true)
  }

  it("TypeConversionMacros: NoData maps across cell types, data passes through") {
    CallSites.b2f(Byte.MinValue).isNaN should be (true)
    CallSites.b2f(5.toByte) should be (5.0f)
    CallSites.i2d(Int.MinValue).isNaN should be (true)
    CallSites.d2i(Double.NaN) should be (Int.MinValue)
    CallSites.d2i(5.0) should be (5)
    CallSites.ub2i(255.toByte) should be (255)
  }

  it("TypeConversionMacros: the argument is evaluated exactly once") {
    var calls = 0
    def arg: Byte = { calls += 1; 5.toByte }
    CallSites.b2f(arg) should be (5.0f)
    calls should be (1)
  }

  it("TileMacros: map and foreach expand into mapper/visitor instances") {
    val tile = new TestTile(Array(1, 2, 3))
    tile.map((col, _, z) => z * 10 + col).data should be (Array(10, 21, 32))
    tile.mapDouble((_, _, z) => z * 2.0).data should be (Array(2, 4, 6))
    var sum = 0
    tile.foreach((col, _, z) => sum += z + col)
    sum should be (9)
    var dsum = 0.0
    tile.foreachDouble((_, _, z) => dsum += z)
    dsum should be (6.0)
  }

  it("generated MacroCombineFunctions: combine expands into a tile combiner") {
    val tile = new TestCombinableTile
    tile.combine(0, 1, 2)((b0, b1, b2) => b0 + b1 + b2) should be (3)
    tile.combineDouble(0, 1, 2)((b0, b1, b2) => b0 + b1 + b2) should be (3)
  }
}
