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
case class ColimetryTags(
  whitePoints: Option[Array[(Long, Long)]] = None,
  primaryChromaticities: Option[Array[(Long, Long)]] = None,
  transferFunction: Option[Array[Int]] = None,
  transferRange: Option[Array[Int]] = None,
  referenceBlackWhite: Option[Array[Long]] = None
)
