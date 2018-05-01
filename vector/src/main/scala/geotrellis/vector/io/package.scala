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

package geotrellis.vector

import geotrellis.vector.io.wkb.WKB
import geotrellis.vector.io.wkt.WKT

package object io extends io.json.Implicits
    with io.wkb.Implicits
    with io.wkt.Implicits {

  /** Read any WKT or WKB and return the corresponding geometry */
  def readWktOrWkb(s: String): Geometry = {
    if (s.startsWith("\\x"))
      WKB.read(s.drop(2))
    else if (s.startsWith("00") || s.startsWith("01"))
      WKB.read(s)
    else
      WKT.read(s)
  }
}

