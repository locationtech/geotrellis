/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster

import geotrellis._

import org.scalatest.FunSuite

class IntConstantTest extends FunSuite {
  val size = 4

  test("building") {
    val d1 = IntConstant(99, 2, 2)
    val d2 = IntArrayRasterData(Array.fill(size)(99), 2, 2)
    assert(d1 === d2)
  }

  test("basic operations") {
    val d = IntConstant(99, 2, 2)

    assert(d.length === size)
    assert(d.getType === TypeInt)
    assert(d(0) === 99)
    assert(d.applyDouble(0) === 99.0)
  }

  test("map") {
    val d1 = IntConstant(99, 2, 2)
    val d2 = d1.map(_ + 1)

    assert(d2.isInstanceOf[IntConstant])
    assert(d2(0) === 100)
  }
}
