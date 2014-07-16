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

package geotrellis.benchmark.io

import geotrellis._
import geotrellis.raster._
import geotrellis.data._

class ArgReader(path:String) extends FileReader(path) {
  def makeReadState(d:Either[String, Array[Byte]],
                    rasterType:RasterType,
                    rasterExtent:RasterExtent,
                    re:RasterExtent): ReadState = rasterType match {
    case TypeBit => new Int1ReadState(d, rasterExtent, re)
    case TypeByte => new Int8ReadState(d, rasterExtent, re)
    case TypeShort => new Int16ReadState(d, rasterExtent, re)
    case TypeInt => new Int32ReadState(d, rasterExtent, re)
    case TypeFloat => new Float32ReadState(d, rasterExtent, re)
    case TypeDouble => new Float64ReadState(d, rasterExtent, re)
    case t => sys.error(s"datatype $t is not supported")
  }

  def readStateFromCache(b:Array[Byte],
                         rasterType:RasterType,
                         rasterExtent:RasterExtent,
                         targetExtent:RasterExtent) = {
    makeReadState(Right(b), rasterType, rasterExtent, targetExtent)
  }

  def readStateFromPath(rasterType:RasterType,
                        rasterExtent:RasterExtent,
                        targetExtent:RasterExtent):ReadState = {
    makeReadState(Left(path),rasterType,rasterExtent, targetExtent)
  }
}
