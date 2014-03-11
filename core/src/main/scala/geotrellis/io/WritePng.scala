/***
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
 ***/

package geotrellis.io

import geotrellis._
import geotrellis.render._
import geotrellis.render.png._

/**
 * Write out a PNG graphic file to the file system at the specified path.
 */
case class WritePng(r:Op[Raster], path:Op[String],
                    colorBreaks:Op[ColorBreaks],
                    noDataColor:Op[Int])
extends Op4(r, path, colorBreaks, noDataColor) ({
  (r, path, colorBreaks, noDataColor) => {
    val breaks = colorBreaks.limits
    val colors = colorBreaks.colors
    val renderer = Renderer(breaks, colors, noDataColor)
    val r2 = renderer.render(r)
    val bytes = new Encoder(renderer.settings).writePath(path, r2)
    Result(())
  }
})

/**
 * Write out a PNG file of a raster that contains RGBA values)
 */
case class WritePngRgba(r:Op[Raster], path:Op[String]) extends Op2(r, path)({
  (r, path) =>
    val bytes = new Encoder(Settings(Rgba, PaethFilter)).writePath(path, r)
    Result(bytes)
})
