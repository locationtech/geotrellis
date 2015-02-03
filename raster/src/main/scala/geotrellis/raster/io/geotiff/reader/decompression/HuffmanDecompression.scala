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

import geotrellis.raster.io.geotiff.reader._

import monocle.syntax._

import spire.syntax.cfor._

trait HuffmanDecompression {

  implicit class Huffman(matrix: Array[Array[Byte]]) {

    def uncompressHuffman(implicit directory: ImageDirectory): Array[Array[Byte]] = {
      val len = matrix.length
      val arr = Array.ofDim[Array[Byte]](len)
      val fillOrder = (directory &|->
        ImageDirectory._nonBasicTags ^|->
        NonBasicTags._fillOrder get)

      cfor(0)(_ < len, _ + 1) { i =>
        val segment = matrix(i)
        val length = directory.rowsInSegment(i)
        val width = directory.rowSize
        val decompressor = TiffFaxDecompressor(fillOrder, width, length)

        val outputArray = Array.ofDim[Byte]((length * width + 7) / 8)

        decompressor.decode1D(outputArray, segment, 0, length)

        arr(i) = outputArray
      }

      arr
    }

  }
}
