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

import monocle.syntax._
import monocle.Macro._

import geotrellis.io.geotiff._
import geotrellis.io.geotiff.ImageDirectoryLenses._

case class T4Options(options: Int = 0, fillOrder: Int)

object GroupThreeDecompression {

  implicit class GroupThree(matrix: Vector[Vector[Byte]]) {

    def uncompressGroupThree(implicit directory: ImageDirectory): Vector[Vector[Byte]] = {
      implicit val t4Options = T4Options(directory |-> t4OptionsLens get,
        directory |-> fillOrderLens get)
      matrix.zipWithIndex.par.map{ case(segment, i) =>
        uncompressGroupThreeSegment(segment, i) }.toVector
    }

    private def uncompressGroupThreeSegment(segment: Vector[Byte], index: Int)
      (implicit t4Options: T4Options, directory: ImageDirectory) = {

      val length = directory.rowsInSegment(index)
      val width = directory.rowSize

      val decompressor = new TIFFFaxDecoder(t4Options.fillOrder, width, length)

      val inputArray = segment.toArray
      val outputArray = Array.ofDim[Byte]((length * width + 7) / 8)

      decompressor.decode2D(outputArray, inputArray, 0, length,
        t4Options.options)

      outputArray.toVector
    }

  }

}
