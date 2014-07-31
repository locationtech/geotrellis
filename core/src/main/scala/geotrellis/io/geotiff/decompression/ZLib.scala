/*
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
 */

package geotrellis.io.geotiff.decompression

import java.util.zip.Inflater

import geotrellis.io.geotiff._

object ZLibDecompression {

  implicit class ZLib(matrix: Vector[Vector[Byte]]) {

    def uncompressZLib(implicit directory: ImageDirectory): Vector[Vector[Byte]] =
      matrix.zipWithIndex.par.map{ case(segment, i) =>
        uncompressZLibSegment(segment, i) }.toVector

    private def uncompressZLibSegment(segment: Vector[Byte], index: Int)
      (implicit directory: ImageDirectory) = {
      val segmentArray = segment.toArray

      try {
        val decompressor = new Inflater()

        decompressor.setInput(segmentArray, 0, segmentArray.length)

        val resultSize = directory.imageSegmentBitsSize(Some(index)) / 8
        val result = new Array[Byte](resultSize.toInt)
        decompressor.inflate(result)
        result.toVector
      } catch {
        case e: Exception =>
          throw new MalformedGeoTiffException("bad zlib compression")
      }
    }

  }

}
