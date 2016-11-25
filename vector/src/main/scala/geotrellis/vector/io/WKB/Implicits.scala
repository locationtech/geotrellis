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

package geotrellis.vector.io.wkb

import geotrellis.util.MethodExtensions
import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class WKBWrapper(val self: Geometry) extends MethodExtensions[Geometry] {
    def toWKB(srid: Int = 0): Array[Byte] =
      WKB.write(self, srid)
  }

  implicit class WKBArrayWrapper(val self: Array[Byte]) extends MethodExtensions[Array[Byte]] {
    def readWKB(): Geometry =
      WKB.read(self)
  }

  implicit class WKHexStringWrapper(val self: String) extends MethodExtensions[String] {
    def readWKB(): Geometry =
      WKB.read(self)
  }
}
