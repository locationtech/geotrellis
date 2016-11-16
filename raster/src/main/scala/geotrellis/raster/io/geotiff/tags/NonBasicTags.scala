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

package geotrellis.raster.io.geotiff.tags

import monocle.macros.Lenses

@Lenses("_")
case class NonBasicTags(
  cellLength: Option[Int] = None,
  cellWidth: Option[Int] = None,
  extraSamples: Option[Array[Int]] = None,
  fillOrder: Int = 1,
  freeByteCounts: Option[Array[Long]] = None,
  freeOffsets: Option[Array[Long]] = None,
  grayResponseCurve: Option[Array[Int]] = None,
  grayResponseUnit: Option[Int] = None,
  newSubfileType: Option[Long] = None,
  orientation: Int = 1,
  planarConfiguration: Option[Int] = None,
  subfileType: Option[Int] = None,
  thresholding: Int = 1,
  t4Options: Int = 0,
  t6Options: Option[Int] = None,
  halftoneHints: Option[Array[Int]] = None,
  predictor: Option[Int] = None
)
