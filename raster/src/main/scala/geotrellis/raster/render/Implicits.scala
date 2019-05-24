/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.render

import geotrellis.raster.{Tile, MultibandTile}


object Implicits extends Implicits

trait Implicits {
  implicit class RGBA(val int: Int) {
    def red = (int >> 24) & 0xff
    def green = (int >> 16) & 0xff
    def blue = (int >> 8) & 0xff
    def alpha = int & 0xff
    def isOpaque = (alpha == 255)
    def isTransparent = (alpha == 0)
    def isGrey = (red == green) && (green == blue)
    def unzip = (red, green, blue, alpha)
    def toARGB = (int >> 8) | (alpha << 24)
    def unzipRGBA: (Int, Int, Int, Int) = (red, green, blue, alpha)
    def unzipRGB: (Int, Int, Int) = (red, green, blue)
  }

  implicit class withSinglebandRenderMethods(val self: Tile) extends ColorMethods
    with JpgRenderMethods
    with PngRenderMethods
    with AsciiRenderMethods

  implicit class withMultibandRenderMethods(val self: MultibandTile) extends MultibandColorMethods
    with MultibandJpgRenderMethods
    with MultibandPngRenderMethods
}
