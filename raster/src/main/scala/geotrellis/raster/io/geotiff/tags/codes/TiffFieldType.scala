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

package geotrellis.raster.io.geotiff.tags.codes

object TiffFieldType {

  val BytesFieldType = 1
  val AsciisFieldType = 2
  val ShortsFieldType = 3
  val IntsFieldType = 4
  val FractionalsFieldType = 5
  val SignedBytesFieldType = 6
  val UndefinedFieldType = 7
  val SignedShortsFieldType = 8
  val SignedIntsFieldType = 9
  val SignedFractionalsFieldType = 10
  val FloatsFieldType = 11
  val DoublesFieldType = 12
  val LongsFieldType = 16 // This is 64-bits
  val SignedLongsFieldType = 17
  val IFDOffset = 18
}
