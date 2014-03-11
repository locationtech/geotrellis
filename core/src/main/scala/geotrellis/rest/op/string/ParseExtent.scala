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

package geotrellis.rest.op.string

import geotrellis._

/**
 * Parses a string into an extent. String must be in the format of (xmin,ymin,xmax,ymax).
 */
case class ParseExtent(s:Op[String]) extends Op1(s)({
  s => try {
    val Array(x1, y1, x2, y2) = s.split(",").map(_.toDouble)
    Result(Extent(x1, y1, x2, y2))
  } catch {
    case e:Exception => sys.error(s"Could not parse extent $s: $e")
  }
})
