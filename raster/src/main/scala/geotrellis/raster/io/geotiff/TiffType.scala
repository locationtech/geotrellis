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

package geotrellis.raster.io.geotiff

abstract sealed class TiffType extends Serializable { val code: Char }

case object Tiff extends TiffType { val code: Char = 42 }
case object BigTiff extends TiffType { val code: Char = 43 }

object TiffType {
  def fromCode(code: Char): TiffType = code match {
    case Tiff.code => Tiff
    case BigTiff.code => BigTiff
  }
}