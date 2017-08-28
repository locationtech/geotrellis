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

abstract sealed class NewSubfileType extends Serializable { val code: Int }

case object ReducedImage extends NewSubfileType { val code = 1 }
case object Page extends NewSubfileType { val code = 2 }
case object Mask extends NewSubfileType { val code = 4 }

object NewSubfileType {
  def fromCode(code: Long): NewSubfileType = fromCode(code.toInt)
  def fromCode(code: Int): NewSubfileType = code match {
    case ReducedImage.code => ReducedImage
    case Page.code => Page
    case Mask.code => Mask
  }
}