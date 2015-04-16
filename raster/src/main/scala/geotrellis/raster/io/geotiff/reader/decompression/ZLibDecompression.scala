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

package geotrellis.raster.io.geotiff.reader.decompression

import java.util.zip.Inflater

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags.Tags

import spire.syntax.cfor._

trait ZLibDecompression {

  implicit class ZLib(matrix: Array[Array[Byte]]) {
    def uncompressZLib(directory: Tags): Array[Array[Byte]] = {
      val len = matrix.length
      val arr = Array.ofDim[Array[Byte]](len)

      try {
        cfor(0)(_ < len, _ + 1) { i =>
          val segment = matrix(i)

          val decompressor = new Inflater()

          decompressor.setInput(segment, 0, segment.length)

          println(directory.imageSegmentByteSize(Some(i)))
          val resultSize = directory.imageSegmentByteSize(Some(i))
          val result = new Array[Byte](resultSize.toInt)
          decompressor.inflate(result)
          arr(i) = result
        }
      } catch {
        case e: Exception =>
          throw new MalformedGeoTiffException("bad zlib compression")
      }

      arr
    }
  }

}
