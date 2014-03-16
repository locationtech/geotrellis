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

package geotrellis.io

import geotrellis._
import geotrellis.data.geotiff._

/**
  * Render a raster as a GeoTiff.
  */
case class RenderGeoTiff(r:Op[Raster], compression:Compression) extends Op1(r) ({
  r => {
    val settings = r.rasterType match {
      case TypeBit | TypeByte => Settings(ByteSample, Signed, true, compression)
      case TypeShort => Settings(ShortSample, Signed, true, compression)
      case TypeInt => Settings(IntSample, Signed, true, compression)
      case TypeFloat => Settings(IntSample, Floating, true, compression)
      case TypeDouble => Settings(LongSample, Floating, true, compression)
    }
    Result(Encoder.writeBytes(r, settings))
  }
})

