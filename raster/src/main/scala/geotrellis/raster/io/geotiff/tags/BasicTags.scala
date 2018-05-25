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
case class BasicTags(
  bitsPerSample: Int = 1, // This is written as an array per sample, but libtiff only takes one value, and so do we.
  colorMap: Seq[(Short, Short, Short)] = Seq(),
  imageLength: Int = 0,
  imageWidth: Int = 0,
  compression: Int = 1,
  photometricInterp: Int = -1,
  resolutionUnit: Option[Int] = None,
  rowsPerStrip: Long = -1, // If it's undefined GDAL interprets the entire TIFF as a single strip
  samplesPerPixel: Int = 1,
  stripByteCounts: Option[Array[Long]] = None,
  stripOffsets: Option[Array[Long]] = None,
  xResolution: Option[(Long, Long)] = None,
  yResolution: Option[(Long, Long)] = None
)
