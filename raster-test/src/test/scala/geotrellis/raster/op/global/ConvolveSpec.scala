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

package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.testkit._

import org.scalatest._

class ConvolveSpec extends FunSuite with RasterMatchers {
  def doit(in1: Array[Int], in2: Array[Int], out: Array[Int]) = {
    val size1 = math.sqrt(in1.length).toInt    
    assert(size1 * size1 === in1.length)

    val size2 = math.sqrt(in2.length).toInt
    assert(size2 * size2 === in2.length)

    val e1 = Extent(0, 0, 10 * size1, 10 * size1)
    val e2 = Extent(0, 0, 10 * size2, 10 * size2)
    val re1 = RasterExtent(e1, 10, 10, size1, size1)
    val re2 = RasterExtent(e2, 10, 10, size2, size2)

    val tile1 = IntArrayTile(in1, size1, size1)
    val tile2 = IntArrayTile(in2, size2, size2)

    val tile3 = tile1.convolve(tile2)

    assert(tile3.toArray === out)
  }
  
  test("gaussian") {
    // Create and sample a 5x5 guassian
    val k1 = Kernel.gaussian(5, 2.0, 100.0)

    // (3, 1) => (1, 1) => r = sqrt(1 * 1 + 1 * 1) = sqrt(2)
    // 100 * exp(-(sqrt(2)^2) / (2 * (2.0^2))) = 77.88 = 77
    assert(k1.tile.get(3, 1) === 77)

    // Should also be symmetric
    assert(k1.tile.get(3, 3) === 77)
    assert(k1.tile.get(3, 1) === 77)
    assert(k1.tile.get(1, 3) === 77)
    assert(k1.tile.get(1, 1) === 77)

    // Make sure amp and sigma do stuff
    val k2 = Kernel.gaussian(5, 4.0, 50.0)

    // (3, 1) => (1, 1) => r = sqrt(1 * 1 + 1 * 1) = sqrt(2)
    // 50 * exp(-(sqrt(2)^2) / (2 * (4.0^2))) = 46.97 = 46
    assert(k2.tile.get(3, 1) === 46)
  }
 

  test("simple convolve") {
    /* http://www.songho.ca/dsp/convolution/convolution2d_example.html */
    doit(Array(1, 2, 3,
               4, 5, 6,
               7, 8, 9),
         Array(-1, -2, -1,
               0, 0, 0,
               1, 2, 1),
         Array(-13, -20, -17,
               -18, -24, -18,
               13, 20, 17))
  }

  test("impulse") {
    // Impulse test
    val a = Array(1, 2, 3, 4,
               5, 6, 7, 8,
               9, 10, 11, 12,
               13, 14, 15, 16)
    doit(a,
         Array(0, 0, 0,
               0, 1, 0,
               0, 0, 0),
         a)
  }
}
