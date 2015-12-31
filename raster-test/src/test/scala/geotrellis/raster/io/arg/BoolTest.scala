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

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

class BoolTest extends FunSuite 
                  with RasterMatchers {
  test("write out the bit raster") {
    val path = "/tmp/foo-bool.arg"
    val arr = Array[Byte]((1 + 0 + 4 + 0 + 0 + 32 + 64 + 128).toByte,
                          (0 + 0 + 0 + 8 + 16 + 0 + 64 + 128).toByte)
    val tile = BitArrayTile(arr, 4, 4)
    ArgWriter(TypeBit).writeData(path, tile)
    val result = ArgReader.read(path, TypeBit, 4, 4)
    assertEqual(result, tile)
  }
}
