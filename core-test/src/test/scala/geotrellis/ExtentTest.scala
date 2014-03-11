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

package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class ExtentTest extends FunSuite with ShouldMatchers {
  test("invalid ranges") {
    intercept[ExtentRangeError] { Extent(10.0, 0.0, 0.0, 10.0) }
    intercept[ExtentRangeError] { Extent(0.0, 10.0, 10.0, 0.0) }
  }

  test("comparing extents") {
    val e1 = Extent(0.0, 0.0, 10.0, 10.0)
    val e2 = Extent(0.0, 20.0, 10.0, 30.0)
    val e3 = Extent(20.0, 0.0, 30.0, 10.0)
    val e4 = Extent(0.0, 0.0, 20.0, 20.0)
    val e5 = Extent(0.0, 0.0, 10.0, 30.0)

    assert((e1 compare e1) === 0)
    assert((e1 compare e2) === -1)
    assert((e1 compare e3) === -1)
    assert((e1 compare e4) === -1)
    assert((e1 compare e5) === -1)

    assert((e2 compare e1) === 1)
    assert((e2 compare e2) === 0)
    assert((e2 compare e3) === 1)
    assert((e2 compare e4) === 1)
    assert((e2 compare e5) === 1)

    assert((e3 compare e1) === 1)
    assert((e3 compare e2) === -1)
    assert((e3 compare e3) === 0)
    assert((e3 compare e4) === 1)
    assert((e3 compare e5) === 1)

    assert((e4 compare e1) === 1)
    assert((e4 compare e2) === -1)
    assert((e4 compare e3) === -1)
    assert((e4 compare e4) === 0)
    assert((e4 compare e5) === -1)

    assert((e5 compare e1) === 1)
    assert((e5 compare e2) === -1)
    assert((e5 compare e3) === -1)
    assert((e5 compare e4) === 1)
    assert((e5 compare e5) === 0)
  }

  test("combining extents") {
    val e1 = Extent(0.0, 0.0, 10.0, 10.0)
    val e2 = Extent(20.0, 0.0, 30.0, 10.0)
    val e3 = Extent(0.0, 0.0, 30.0, 10.0)
    assert(e1.combine(e2) === e3)
  }

  test("contains interior points") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(3.0, 3.0) === true)
    assert(e.containsPoint(0.00001, 9.9999) === true)
  }

  test("doesn't contain exterior points") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(100.0, 0.0) === false)
    assert(e.containsPoint(0.0, 1000.0) === false)
  }

  test("doesn't contain boundary") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(0.0, 0.0) === false)
    assert(e.containsPoint(0.0, 3.0) === false)
    assert(e.containsPoint(0.0, 10.0) === false)
    assert(e.containsPoint(10.0, 0.0) === false)
    assert(e.containsPoint(10.0, 10.0) === false)
  }

  test("get corners") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.southWest === (0.0, 0.0))
    assert(e.northEast === (10.0, 10.0))
  }

  test("containsExtent") {
    val e = Extent(0.0, 100.0, 10.0, 200.0)
    assert(e.containsExtent(e))
    assert(e.containsExtent(Extent(1.0, 102.0, 9.0,170.0))) 
    assert(!e.containsExtent(Extent(-1.0, 102.0, 9.0,170.0))) 
    assert(!e.containsExtent(Extent(1.0, -102.0, 9.0,170.0)))
    assert(!e.containsExtent(Extent(1.0, 102.0, 19.0,170.0))) 
    assert(!e.containsExtent(Extent(1.0, 102.0, 9.0,370.0))) 
  }

  test("intersects") {
    val base = Extent(0.0, -20.0, 100.0, -10.0)

    def does(other:geotrellis.Extent) =
      base.intersects(other) should be (true)

    def doesnot(other:geotrellis.Extent) =
      base.intersects(other) should be (false)

    doesnot(Extent(-100.0,-20.0,-1.0,-10.0))
    does(Extent(-100.0,-20.0,0.0,-10.0))
    does(Extent(-100.0,-20.0,10.0,-10.0))
    does(Extent(0.0,-20.0,10.0,-10.0))
    does(Extent(40.0,-20.0,120.0,-10.0))
    does(Extent(100.0,-20.0,120.0,-10.0))
    doesnot(Extent(110.0,-20.0,120.0,-10.0))
    does(Extent(-100.0,-20.0,120.0,-10.0))
    doesnot(Extent(-100.0,-30.0,120.0,-21.0))
    does(Extent(-100.0,-30.0,120.0,-20.0))
    does(Extent(-100.0,-15.0,120.0,-10.0))
    does(Extent(-100.0,-15.0,120.0,0.0))
    doesnot(Extent(-100.0,-9.0,120.0,0.0))
    doesnot(Extent(-100.0,-9.0,-10.0,0.0))
  }
}
