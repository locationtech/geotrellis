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

package geotrellis.render.op

import geotrellis._
import geotrellis.render.png._

/**
 * Generate a PNG from a raster of RGBA integer values.
 *
 * Use this operation when you have created a raster whose values are already
 * RGBA color values that you wish to render into a PNG.  If you have a raster
 * with data that you wish to render, you should use RenderPng instead.
 *
 * An RGBA value is a 32 bit integer with 8 bits used for each component:
 * the first 8 bits are the red value (between 0 and 255), then green, blue,
 * and alpha (with 0 being transparent and 255 being opaque).
 */
case class RenderPngRgba(r:Op[Raster]) extends Op1(r)({
  r =>
    val bytes = new Encoder(Settings(Rgba, PaethFilter)).writeByteArray(r)
    Result(bytes)
})
