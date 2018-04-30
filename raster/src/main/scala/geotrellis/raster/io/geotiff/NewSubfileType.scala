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

/**
  * A general indication of the kind of data contained in this subfile.
  * URL: https://www.awaresystems.be/imaging/tiff/tifftags/newsubfiletype.html
  */
abstract sealed class NewSubfileType extends Serializable { val code: Int }

/** Reduced-resolution version of another image in this TIFF file */
case object ReducedImage extends NewSubfileType { val code = 1 }
/** Single page of a multi-page image (see the PageNumber field description) */
case object Page extends NewSubfileType { val code = 2 }
/** Transparency mask for another image in this TIFF file */
case object Mask extends NewSubfileType { val code = 4 }

object NewSubfileType {
  def fromCode(code: Long): Option[NewSubfileType] = fromCode(code.toInt)
  def fromCode(code: Int): Option[NewSubfileType] = code match {
    case ReducedImage.code => Some(ReducedImage)
    case Page.code => Some(Page)
    case Mask.code => Some(Mask)
    case _ => None
  }
}
