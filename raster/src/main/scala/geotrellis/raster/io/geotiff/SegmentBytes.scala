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
  * Represents the bytes of the segments with-in a GeoTiff.
  *
  * The base trait of SegmentBytes. It can be implemented either as
  * an Array[Array[Byte]] or as a ByteBuffer that is lazily read in.
  */
trait SegmentBytes extends Seq[Array[Byte]] with Serializable {
  def getSegment(i: Int): Array[Byte]

  def getSegments(indices: Traversable[Int]): Iterator[(Int, Array[Byte])]

  def getSegmentByteCount(i: Int): Int

  def apply(idx: Int): Array[Byte] = getSegment(idx)

  /** Provides and iterator for segments. Do not assume the segments are in a particular order. */
  def iterator: Iterator[Array[Byte]] =
    getSegments(0 until length).map(_._2)
}
